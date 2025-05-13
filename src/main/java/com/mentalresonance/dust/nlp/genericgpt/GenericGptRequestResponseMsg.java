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

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedHashMap;

/**
 * Most GPT interfaces are based on the ChatGPT model. This abstracts out most of the boilerplate
 */
public abstract class GenericGptRequestResponseMsg extends GPTMsg // extends ReturnableMsg
{
    /**
     * Default prompt
     */
    public static final String DEFAULT_SYSTEM_PROMPT = "You are ChatGPT, a large language model by OpenAI";
    /**
     * A default model
     */
    public static final String MISTRAL = "mistral-nemo";

    /**
     * A default model
     */
    public static final String PHI4 = "PHI4";

    /**
     * System prompt to use
     */
    @Getter
    protected String systemPrompt = DEFAULT_SYSTEM_PROMPT;


    /**
     * Maximum number of completion tokens
     */
    @Getter
    protected int maxTokens = 4096;

    /**
     * Accounting
     */
    @Getter
    protected Integer promptTokens, completionTokens, totalTokens;

    /**
     * Temperature - we default to 0
     */
    @Getter
    @Setter
    protected float temperature = 0.0f;

    /**
     * The response - a convoluted map
     */
    public LinkedHashMap<Object, Object> response = null;

    /**
     * Constructor
     */
    public GenericGptRequestResponseMsg() {}

    /**
     * Constructor
     * @param request the request
     */
    public GenericGptRequestResponseMsg(String request) { this.request = request; }

    /**
     * Constructor
     * @param request the request
     * @param maxTokens max tokens in completion
     */
    public GenericGptRequestResponseMsg(String request, int maxTokens) {
        this.request = request;
        this.maxTokens = maxTokens;
    }

    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     */
    public GenericGptRequestResponseMsg(String request, String systemPrompt) {
        this.request = request;
        this.systemPrompt = systemPrompt;
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     * @param maxTokens max tokens in completion
     */
    public GenericGptRequestResponseMsg(String request, String systemPrompt, int maxTokens) {
        this.request = request;
        this.systemPrompt = systemPrompt;
        this.maxTokens = maxTokens;
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     * @param model the LLM model to use
     */
    public GenericGptRequestResponseMsg(String request, String systemPrompt, String model) {
        this.request = request;
        this.systemPrompt = systemPrompt;
        this.model = model;
    }
    /**
     * Constructor
     * @param request the request
     * @param systemPrompt the system prompt
     * @param model the LLM model to use
     * @param maxTokens max tokens in completion
     */
    public GenericGptRequestResponseMsg(String request, String systemPrompt, String model, int maxTokens) {
        this.request = request;
        this.systemPrompt = systemPrompt;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    /**
     * Our Overlord speaks.
     * @return The utterance
     * @throws GenericGptException on error
     */
    abstract public String getUtterance() throws GenericGptException;

    /**
     * Has the message been sent off to the LLM ?
     * @return true if any form of processing is evident else false
     */
    public boolean isProcessed() { return response != null; }

    @Override
    public String toString() {
        return "%s: %s".formatted(this.getClass(), request);
    }
}
