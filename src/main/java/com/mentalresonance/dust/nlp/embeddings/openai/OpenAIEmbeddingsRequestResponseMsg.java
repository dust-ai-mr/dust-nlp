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

import com.mentalresonance.dust.nlp.genericgpt.GPTMsg;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
public class OpenAIEmbeddingsRequestResponseMsg extends GPTMsg {

    Integer length;

    @Setter
    List<Double> response;

    public OpenAIEmbeddingsRequestResponseMsg(String model, String request, Integer length) {
        this.model = model;
        this.request = request;
        this.length = length;
    }

    public OpenAIEmbeddingsRequestResponseMsg(String model, String request) {
        this(model, request, null);
    }

    public OpenAIEmbeddingsRequestResponseMsg(String request) {
        this("text-embedding-3-small", request, null);
    }
}
