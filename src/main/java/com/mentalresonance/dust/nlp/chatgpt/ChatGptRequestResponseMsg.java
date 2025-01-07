/*
 *
 *  Copyright 2024-2025 Alan Littleford
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

/**
 * Concrete ChatGPT request class
 */
public class ChatGptRequestResponseMsg extends GenericGptRequestResponseMsg
{
    /**
     * Models
     */
    public static final String GPT_3_5 = "gpt-3.5-turbo-0125";
    /**
     * Models
     */
    public static final String GPT_4 = "gpt-4";
    /**
     * Models
     */
    public static final String GPT_4o = "gpt-4o";

    public static final String GPT_4o_latest = "gpt-4o-2024-08-06";

    /**
     * Default system prompt
     */
    public static final String DEFAULT_SYSTEM_PROMPT = "You are ChatGPT, a large language model by OpenAI";

    /**
     * Constructor
     */
    public ChatGptRequestResponseMsg() {};
    /**
     * Constructor
     * @param request the request
     */
    public ChatGptRequestResponseMsg(String request) {
        super(request);
        systemPrompt = DEFAULT_SYSTEM_PROMPT;
        model = GPT_4o;
    }
    /**
     * Constructor
     * @param request the request
     * @param maxTokens max tokens in completion
     */
    public ChatGptRequestResponseMsg(String request, int maxTokens) {
        super(request, maxTokens);
        systemPrompt = DEFAULT_SYSTEM_PROMPT;
        model = GPT_4o;
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     */
    public ChatGptRequestResponseMsg(String request, String systemPrompt) {
        super(request, systemPrompt);
        model = GPT_4o;
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     * @param maxTokens max tokens in completion
     */
    public ChatGptRequestResponseMsg(String request, String systemPrompt, int maxTokens) {
        super(request, systemPrompt, maxTokens);
        model = GPT_4o;
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     * @param model the LLM model to use
     */
    public ChatGptRequestResponseMsg(String request, String systemPrompt, String model) {
        super(request, systemPrompt, model);
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     * @param model the LLM model to use
     * @param maxTokens max tokens in completion
     */
    public ChatGptRequestResponseMsg(String request, String systemPrompt, String model, int maxTokens) {
        super(request, systemPrompt, model, maxTokens);
    }
    /**
     * Our Overlord speaks.
     * *Careful* this has to be called before costs are updated. Todo: clean this up
     * @return The utterance
     * @throws GenericGptException on error
     */
    public String getUtterance() throws GenericGptException {
        String utterance;
        Map error;

        if (null != response) {
            if (null != (error = (Map)(response.get("error")))) {
                throw new GenericGptException((String)error.get("message"));
            }
            else {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                Map<String, Double> usage = (Map<String, Double>) response.get("usage");

                if (null != choices && choices.size() > 0) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    utterance = (null != message) ? message.get("content").toString() : null;
                    promptTokens = usage.get("prompt_tokens").intValue();
                    completionTokens = usage.get("completion_tokens").intValue();
                    totalTokens = usage.get("total_tokens").intValue();
                } else
                    throw new GenericGptException("No choices in response");
            }
        } else
            throw new GenericGptException("GPT: No response for utterance: %s".formatted(request));

        return utterance;
    }
}
