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
	 * @return text to display or null.
	 *
	 * Note that the completions API and the responses API use a different protocol, we deal with this here.
	 */
	public static String streamingText(StreamingHttpDataMsg msg) {
		String text = null;

		if (!msg.getData().equals("[DONE]")) {
			try {
				Gson gson = new Gson();
				LinkedHashMap<String, Object> m = gson.fromJson(msg.getData(), LinkedHashMap.class);
				if (m != null) {
					if (m.containsKey("choices")) { // Completions response
						List<Map<String, Object>> choices = (List<Map<String, Object>>) m.get("choices");
						if (choices != null && !choices.isEmpty()) {
							Map<String, Object> choice = choices.get(0);
							if (choice.containsKey("delta")) {
								Map<String, String> delta = (Map<String, String>) choice.get("delta");
								if (delta != null && delta.containsKey("content")) {
									String content = delta.get("content");
									if (content != null) {
										text = content;
									}
								}
							}
						}
					}
					else if (m.containsKey("delta")) {  // Responses response
						text = m.get("delta").toString();
					}
				}
			} catch (Exception e) {
				log.error("{} utterance: {}", e.getMessage(), msg.getData());
			}
		}
		if (text != null) {
			text = text.replaceAll("\n", "<br>");
		}
		return text;
	}
}
