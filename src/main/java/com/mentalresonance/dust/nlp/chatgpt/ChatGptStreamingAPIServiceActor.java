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

package com.mentalresonance.dust.nlp.chatgpt;

import com.google.gson.Gson;
import com.mentalresonance.dust.core.actors.ActorBehavior;
import com.mentalresonance.dust.core.actors.ActorRef;
import com.mentalresonance.dust.core.actors.Props;
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;
import com.mentalresonance.dust.nlp.genericgpt.GenericGptStreamingAPIServiceActor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.sse.EventSource;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;



/**
 * Requests/Responses to chat GPT
 * Should be used under a service manager (i.e. it processes one request then stops)
 */
@Slf4j
public class ChatGptStreamingAPIServiceActor extends GenericGptStreamingAPIServiceActor implements HttpClientActor {

	String key;
	EventSource eventSource = null;

	/**
	 * Props
	 * @param proxyThrottler nullable throttler
	 * @param key API key
	 * @return Props
	 */
	public static Props props(ActorRef proxyThrottler, String key) {
		return Props.create(ChatGptStreamingAPIServiceActor.class, proxyThrottler, key);
	}
	/**
	 * Props
	 * @param throttler nullable throttler
	 * @param key API key
	 */
    public ChatGptStreamingAPIServiceActor(ActorRef throttler, String key) {
		super("https://api.openai.com/v1/chat/completions", throttler);
		this.key = key;
	}


	@Override
	public ActorBehavior createBehavior() {
		return (Serializable message) -> {
			switch(message) {
				case ChatGptRequestResponseMsg msg:
					originalSender = sender;
					originalRequest = msg;

					Map<String, Object> data = Map.of(
							"model",  msg.getModel(),
							"messages", List.of(
									Map.of("role", "system", "content", msg.getSystemPrompt()),
									Map.of("role", "user", "content", msg.getRequest())
							) ,
							"temperature",  msg.getTemperature(),
							"max_tokens",  msg.getMaxTokens(),
							"stream", true
					);

					Request gptRequest = HttpService.buildPostRequest(
							api,
							new Gson().toJson(data, LinkedHashMap.class),
							Map.of(
									"Authorization", "Bearer " + key,
									"Content-Type",  "application/json",
									"Accept", "application/json"
							)
					);

					HttpRequestResponseMsg rrm = new HttpRequestResponseMsg(self, gptRequest);

					if (null != throttler) {
						throttler.tell(rrm, sender);
					} else
						eventSource = request(rrm, sender, self);

					originalSender.tell(new ChatGPTStartedStreamingMsg(), self);
					break;

				default: super.createBehavior().onMessage(message);
			}
		};
	}
}

