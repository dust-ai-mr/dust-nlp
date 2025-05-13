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

package com.mentalresonance.dust.nlp.chatgpt;

import com.mentalresonance.dust.nlp.genericgpt.GenericGptException;
import com.mentalresonance.dust.nlp.genericgpt.GenericGptRequestResponseMsg;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Concrete ChatGPT request class
 */
public class ResponsesGptRequestResponseMsg extends GenericGptRequestResponseMsg
{
    public Map<String, Object> options = null;

    /**
     * Constructor
     */
    public ResponsesGptRequestResponseMsg() {};
    /**
     * Constructor
     * @param request the request
     */
    public ResponsesGptRequestResponseMsg(String request, String model, Map<String, Object> options) {
        super(request);
        this.model = model.trim();
        this.options = options;
    }

    public ResponsesGptRequestResponseMsg(String request, String model) {
        super(request);
        this.model = model.trim();
        this.options = Map.of(
            "tools", List.of(
                    Map.of("type", "web_search",
                           "search_context_size", "low"
                    )
                )
            );
    }


    public String getUtterance() throws GenericGptException {
        String utterance;
        Map error;

        if (null != response) {
            if (null != (error = (Map)(response.get("error")))) {
                throw new GenericGptException((String)error.get("message"));
            }
            else {
                List<Map<String, Object>> output = (List<Map<String, Object>>) response.get("output");
                Map<String, Double> usage = (Map<String, Double>) response.get("usage");

                if (null != output && output.size() > 0) {
                    Optional<Map<String, Object>> message = output.stream()
                        .filter(map -> map.get("type").equals("message"))
                        .findFirst();
                    if (message.isPresent()) {
                        List<Map<String, Object>> content = (List<Map<String, Object>>) message.get().get("content");
                        Optional<Map<String, Object>> text = content.stream()
                            .filter(map -> map.get("type").equals("output_text"))
                            .findFirst();
                        if (text.isPresent()) {
                            utterance = text.get().get("text").toString();
                        } else
                            throw new GenericGptException("Response: no utterance found");
                    } else
                        throw new GenericGptException("Response: no message found");

                    promptTokens = usage.get("input_tokens").intValue();
                    completionTokens = usage.get("output_tokens").intValue();
                    totalTokens = usage.get("total_tokens").intValue();
                } else
                    throw new GenericGptException("No output in response");
            }
        } else
            throw new GenericGptException("Responses: No response for utterance: %s".formatted(request));

        return utterance;
    }
}
