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

package com.mentalresonance.dust.nlp.genericgpt.ollama;

import com.mentalresonance.dust.nlp.genericgpt.GenericGptException;
import com.mentalresonance.dust.nlp.genericgpt.GenericGptRequestResponseMsg;

import java.util.Map;

/**
 * A request message for the Ollama LLM engine
 */
public class OllamaRequestResponseMsg extends GenericGptRequestResponseMsg
{
    /**
     * Constructor
     */
    public OllamaRequestResponseMsg() {};
    /**
     * Constructor
     * @param request the request
     */
    public OllamaRequestResponseMsg(String request) { super(request); }
    /**
     * Constructor
     * @param request the request
     * @param maxTokens max tokens in completion
     */
    public OllamaRequestResponseMsg(String request, int maxTokens) {
        super(request, maxTokens);
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     */
    public OllamaRequestResponseMsg(String request, String systemPrompt) {
        super(request, systemPrompt);
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     * @param maxTokens max tokens in completion
     */
    public OllamaRequestResponseMsg(String request, String systemPrompt, int maxTokens) {
        super(request, systemPrompt, maxTokens);
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     * @param model the LLM model to use
     */
    public OllamaRequestResponseMsg(String request, String systemPrompt, String model) {
        super(request, systemPrompt, model);
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     * @param model the LLM model to use
     * @param maxTokens max tokens in completion
     */
    public OllamaRequestResponseMsg(String request, String systemPrompt, String model, int maxTokens) {
        super(request, systemPrompt, model, maxTokens);
    }

    /**
     * Our Overlord speaks.
     * @return The utterance
     * @throws GenericGptException on error
     */
    public String getUtterance() throws GenericGptException {
        String utterance;
        Map<String, Object> errorMap;

        if (null != response) {
            String error = (String)response.get("error");
            if (null != error) {
                throw new GenericGptException(error);
            }
            else {
                utterance = (String) response.get("response");
            }
        } else
            throw new GenericGptException("GPT: No response for utterance: %s".formatted(request));

        return utterance;
    }
}
