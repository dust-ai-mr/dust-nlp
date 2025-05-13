/*
 *
 *  Copyright 2024-Present Alan Littleford
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
 *
 *
 */

package com.mentalresonance.dust.nlp.genericgpt;

import com.google.gson.Gson;
import com.mentalresonance.dust.core.actors.*;
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Requests/Responses to an API that follows the broad outlines of the ChatGPT completions API (which is most).
 */
@Slf4j
public class GenericGptAPIServiceActor extends Actor implements HttpClientActor {

	/**
	 * Completions endpoint
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
	 * Original Sender
	 */
	protected ActorRef originalSender;

	/**
	 * Props
	 * @param api completion endpoint
	 * @param proxyThrottler optional throttler
	 * @return Props
	 */
	public static Props props(String api, ActorRef proxyThrottler) {
		return Props.create(GenericGptAPIServiceActor.class, api, proxyThrottler);
	}
	/**
	 * Constructor
	 * @param api completion endpoint
	 * @param throttler optional throttler
	 */
	public GenericGptAPIServiceActor(String api, ActorRef throttler) {
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
		dieIn(5 * 60 * 1000L);
	}

	@Override
	public void postStop() throws Exception {
		super.postStop();
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
						"stream",  false
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
					if (throttler!= null) {
						throttler.tell(rrm, self);
					} else
						request(rrm);
					break;

				case HttpRequestResponseMsg msg:
					if (null == msg.response && null == msg.exception) {
						// From throttler - so now do request
						request(msg);
					}
					else {
						if (msg.exception != null) {
							if (-- retries > 0) {
								self.tell(originalRequest, originalSender);
							}
							else {
								log.warn("Request {} failed: {}", originalRequest, msg.exception.getMessage());
								stopSelf();
							}
						}
						else {
							String utterance = "Unknown";
							try {
								utterance = msg.response.body().string();
								originalRequest.response = new Gson().fromJson(utterance, LinkedHashMap.class);
							}
							catch (Exception e) {
								log.error( "Error: {} Response from GPT: {}", e.getMessage(), utterance);
								originalRequest.setError(e.getMessage());
							}
						}
						originalSender.tell(originalRequest, parent);
						stopSelf();
					}
					break;

				default: log.error("Got unexpected message {}", message);
			}
		};
	}

	@Override
	protected void dying() {
		log.warn("Gpt did not respond -- stopping service Actor");
		/*
		 * Send message back to service Actor so it can try again
		 */
		parent.tell(originalRequest, originalSender);
	}
}

