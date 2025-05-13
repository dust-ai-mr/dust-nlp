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

package com.mentalresonance.dust.nlp.embeddings.openai;

import com.google.gson.Gson;
import com.mentalresonance.dust.core.actors.Actor;
import com.mentalresonance.dust.core.actors.ActorBehavior;
import com.mentalresonance.dust.core.actors.ActorRef;
import com.mentalresonance.dust.core.actors.Props;
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;
import com.mentalresonance.dust.nlp.chatgpt.ChatGptRequestResponseMsg;
import com.mentalresonance.dust.nlp.genericgpt.GenericGptAPIServiceActor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Requests/Responses to chat GPT
 * Should be used under a service manager (i.e. it processes one request then stops)
 */
@Slf4j
public class OpenAIEmbeddingsAPIServiceActor extends Actor implements HttpClientActor {

	ActorRef originalSender;
	OpenAIEmbeddingsRequestResponseMsg originalRequest;

	static final String api = "https://api.openai.com/v1/embeddings";

	String key;

	ActorRef throttler;

	/**
	 * Props
	 * @param proxyThrottler nullable throttler
	 * @param key API key
	 * @return Props
	 */
	public static Props props(ActorRef proxyThrottler, String key) {
		return Props.create(OpenAIEmbeddingsAPIServiceActor.class, proxyThrottler, key);
	}

	/**
	 * Constructor
	 * @param throttler nullable throttler
	 * @param key API key
	 */
	public OpenAIEmbeddingsAPIServiceActor(ActorRef throttler, String key) {
		this.key = key;
		this.throttler = throttler;
	}

	@Override
	public void preStart() throws Exception {
		super.preStart();
	}

	@Override
	public  ActorBehavior createBehavior() {
		return (Serializable message) -> {
			switch(message) {
				case OpenAIEmbeddingsRequestResponseMsg msg -> {
					originalSender = sender;
					originalRequest = msg;

					Map<String, Object> data = Map.of(
							"model", msg.getModel(),
							"input", msg.getRequest(),
							"encoding_format", "float"
					);
					if (msg.getLength() != null) {
						data.put("dimension", msg.getLength());
					}

					Request gptRequest = HttpService.buildPostRequest(
							api,
							new Gson().toJson(data, LinkedHashMap.class),
							Map.of(
									"Authorization", "Bearer " + key,
									"Content-Type", "application/json",
									"Accept", "application/json"
							)
					);

					HttpRequestResponseMsg rrm = new HttpRequestResponseMsg(self, gptRequest);
					if (null != throttler) {
						throttler.tell(rrm, self);
					} else {
						request(rrm);
					}
				}

				case HttpRequestResponseMsg msg -> {
					if (null == msg.response && null == msg.exception) {
						// From throttler - so now do request
						request(msg);
					}
					else {
						if (msg.exception != null) {
							log.warn("Request {} failed: {}", originalRequest, msg.exception.getMessage());
							stopSelf();
						} else {
							String json = "";
							try {
								json = msg.response.body().string();
								LinkedHashMap<String, Object> response = new Gson().fromJson(json, LinkedHashMap.class);
								if (response.get("error") != null) {
									originalRequest.setError(((Map<String, String>)response.get("error")).get("message"));
								}
								else {
									List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
									originalRequest.response = (List<Double>)data.get(0).get("embedding");
								}
							}
							catch (Exception e) {
								log.error("{} Response from OpenAI Embeddings: {}", e.getMessage(), json);
								originalRequest.setError(e.getMessage());
							}
							finally {
								originalSender.tell(originalRequest, self);
								stopSelf();
							}
						}
					}
				}

				default -> log.error("Unhandled message: {}", message);
			}

		};
	}
}

