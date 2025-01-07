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

package com.mentalresonance.dust.nlp.embeddings;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * Msg to request the entire text be processed (chunked) and the chunks embedded
 */
@Getter
public class EmbeddingsRequestResponseMsg implements Serializable {
    /**
     * Text to embed
     */
    String text;

    /**
     * List of embeddings - one per chunk
     */
    @Setter
    List<Embedding> embeddings = new LinkedList<>();

    /**
     * Constructor
     * @param text to embed
     */
    public EmbeddingsRequestResponseMsg(String text) {
        this.text = text;
    }

    @Override
    public String toString() { return "EmbeddingsRequestResponseMsg: " + embeddings.size() + " chunks."; }
}
