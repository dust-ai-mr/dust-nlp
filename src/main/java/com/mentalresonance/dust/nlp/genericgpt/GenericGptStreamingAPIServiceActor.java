/*
 * Copyright 2024 Alan Littleford
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.mentalresonance.dust.nlp.genericgpt;

import com.google.gson.Gson;
import com.mentalresonance.dust.core.actors.*;
import com.mentalresonance.dust.http.msgs.StreamingHttpEndMsg;
import com.mentalresonance.dust.http.msgs.StreamingHttpFailureMsg;
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.sse.EventSource;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Requests/Responses to an API that follows the Streaming ChatGPT completions API (which is most).
 */
@Slf4j
public class GenericGptStreamingAPIServiceActor extends Actor implements HttpClientActor {

	/**
	 * Completions end point
	 */
	protected String api;
	Integer retries = 3;
	/**
	 * Optional throttler
	 */
	protected ActorRef throttler;
	/**
	 * Original request
	 */
	protected GenericGptRequestResponseMsg originalRequest;
	/**
	 * Original sender
	 */
	protected ActorRef originalSender;
	/**
	 * Event source for Server Side Events (which API uses for streaming)
	 */
	protected EventSource eventSource = null;

	/**
	 * Props
	 * @param api endpoint
	 * @param proxyThrottler optional throttler
	 * @return Props
	 */
	public static Props props(String api, ActorRef proxyThrottler) {
		return Props.create(GenericGptStreamingAPIServiceActor.class, api, proxyThrottler);
	}
	/**
	 * Props
	 * @param api endpoint
	 * @param throttler optional throttler
	 */
	public GenericGptStreamingAPIServiceActor(String api, ActorRef throttler) {
		this.api = api;
		this.throttler = throttler;
	}

	/**
	 * As a service Actor we don't want to just sit forever waiting for GPT if it has gone
	 * away - so timeout here.
	 */
	@Override
	public void preStart() throws Exception {
		super.preStart();
		dieIn(60 * 1000L);
	}

	@Override
	public void postStop() throws Exception {
		super.postStop();
		cancelDeadMansHandle();
	}

	@Override
	public ActorBehavior createBehavior() {
		return (Serializable message) -> {
			switch(message) {
				case GenericGptRequestResponseMsg msg:
					originalSender = sender;
					originalRequest = msg;
					Map<String, Object> data = Map.of(
							"model", msg.model,
							"prompt",  msg.request,
							"temperature",  0.0,
							"max_tokens",  msg.maxTokens,
							"stream",  true
					);
					Request gptRequest = HttpService.buildPostRequest(
							api,
							new Gson().toJson(data, LinkedHashMap.class),
							Map.of(
								"Content-Type", "application/json",
								"Accept",  "application/json"
							)
					);

					HttpRequestResponseMsg rrm = new HttpRequestResponseMsg(self, gptRequest);
					if (null != throttler) {
						throttler.tell(rrm, self);
					} else
						eventSource = request(rrm, sender, self);

					originalSender.tell(new GenericGPTStartedStreamingMsg(), self);
					break;

				case HttpRequestResponseMsg msg:
					if (null == msg.response) {
						// From throttler - so now do request
						request(msg, originalSender, self);
					}
					else {
						if (msg.exception != null) {
							if (-- retries > 0) {
								self.tell(originalRequest, originalSender);
							}
							else {
								log.warn("Request {} failed", originalRequest);
								stopSelf();
							}
						}
						else {
							originalRequest.response = new Gson().fromJson(msg.response.body().string(), LinkedHashMap.class);
							originalSender.tell(originalRequest, parent);
							stopSelf();
						}
					}
					break;

				case StreamingHttpEndMsg ignored:
					eventSource.cancel();
					originalSender.tell(message, self);
					stopSelf();
					break;

				case StreamingHttpFailureMsg msg:
					String error = (null != msg.getT()) ? msg.getT().getMessage() :
							(null != msg.getResponse()) ? msg.getResponse().message(): "Unknown";
					log.error("Streaming failure: {} {}", msg.getResponse().code(), error);
					originalSender.tell(msg, parent);
					stopSelf();
					break;

				default: log.error("Got unexpected message '{}'", message);
			}
		};
	}

	@Override
	protected void dying() {
		log.warn("GPT did not respond -- stopping service Actor");
		/*
		 * Send message back to service Actor so it can try again
		 */
		eventSource.cancel();
		parent.tell(originalRequest, originalSender);
	}
}

