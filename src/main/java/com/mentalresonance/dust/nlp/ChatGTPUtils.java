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

package com.mentalresonance.dust.nlp;

import com.google.gson.Gson;
import com.mentalresonance.dust.http.msgs.StreamingHttpDataMsg;
import lombok.extern.slf4j.Slf4j;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Utilities specifically for ChatGPT
 */
@Slf4j
public class ChatGTPUtils {

	/**
	 * Constructor
	 */
	public ChatGTPUtils() {}

	/**
	 * With a .data string in a streaming message Chat GPT has its own protocol going on
	 * (i.e. msg.data is far from what should be printed). We handle this here
	 * @param msg streaming data
	 * @return text to display or null
	 */
	public static String streamingText(StreamingHttpDataMsg msg) {
		String text = null;

		if (!msg.getData().equals("[DONE]")) {
			try {
				Gson gson = new Gson();
				Map<String, List<Map<String, Object>>> m = gson.fromJson(msg.getData(), LinkedHashMap.class);
				if (m != null && m.containsKey("choices")) {
					List<Map<String, Object>> choices = m.get("choices");
					if (choices != null && !choices.isEmpty()) {
						Map<String, Object> choice = choices.get(0);
						if (choice.containsKey("delta")) {
							Map<String, String> delta = (Map<String, String>) choice.get("delta");
							if (delta != null && delta.containsKey("content")) {
								String content = delta.get("content");
								if (content != null) {
									text = content.replaceAll("\n", "<br>");
								}
							}
						}
					}
				}
			} catch (Exception e) {
				log.error("{} utterance: {}", e.getMessage(), msg.getData());
			}
		}
		return text;
	}
}
