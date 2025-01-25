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

package com.mentalresonance.dust.nlp.embeddings.huggingface;

import com.google.gson.Gson;
import com.mentalresonance.dust.core.actors.*;
import com.mentalresonance.dust.core.msgs.CompletionRequestMsg;
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg;
import com.mentalresonance.dust.http.service.HttpService;
import com.mentalresonance.dust.http.trait.HttpClientActor;
import com.mentalresonance.dust.nlp.embeddings.Embedding;
import com.mentalresonance.dust.nlp.embeddings.EmbeddingsRequestResponseMsg;
import com.mentalresonance.dust.nlp.lang.Words;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Responds to requests to get embeddings for a submitted text. Uses the hugging face embeddings server
 */
@Slf4j
public class HFEmbeddingAPIServiceActor extends Actor implements HttpClientActor {

	// chunksLeft handles async since we just spin and throw embeddings requests at the embedder
	Integer chunksLeft = 0, chunkSize;
	EmbeddingsRequestResponseMsg originalRequest;
	ActorRef originalSender;
	String api;

	/**
	 * Props
	 * @param api url of hugging face api
	 * @param chunkSize size to chunk to
	 * @return Props
	 */
	public static Props props(String api, Integer chunkSize) {
		return Props.create(HFEmbeddingAPIServiceActor.class, api, chunkSize);
	}

	/**
	 * Constructor
	 * @param api url of hugging face api
	 * @param chunkSize size to chunk to
	 */
	public HFEmbeddingAPIServiceActor(String api, Integer chunkSize) {
		this.api = api;
		this.chunkSize = chunkSize;
	}

	@Override
	public void preStart() {
		dieIn(60 * 1000L);
	}

	@Override
	public ActorBehavior createBehavior() {
		return (Serializable message) -> {
			switch(message) {
				case EmbeddingsRequestResponseMsg msg:

					originalSender = sender;
					originalRequest = msg;

					/*
					 * Chunk by sentences with a one sentence overlap between consecutive chunks
					 */
					LinkedList<String> sentences;
					String allText = originalRequest.getText();

					if (allText.length() <= chunkSize) {
						sentences = new LinkedList<>(List.of(allText));
					} else
						sentences = Words.sentences(originalRequest.getText(), Locale.ENGLISH);

                    String newSentence = sentences.removeFirst(), lastSentence = "", text = null;

                    while (newSentence != null)
                    {
						// Starting a new chunk - start with end of last chunk
                        text = lastSentence;
                        int currentSize = text.length();

                        // Build the chunk
                        while (newSentence != null && (currentSize = currentSize + newSentence.length()) < chunkSize) {
                            text = text + newSentence;
                            lastSentence = newSentence;
                            newSentence = !sentences.isEmpty() ? sentences.removeFirst() : null;
                        }
                        if (!text.isEmpty()) {
							if (text.length() < chunkSize / 2 && !sentences.isEmpty())
								log.warn("Sentence contains {} characters", text.length());
                            requestEmbedding(text);
                            ++chunksLeft;
                        }
                        // If we failed because our 'sentence' is too big drop the sentence
						// Note - in this case we lose the overlap
                        if (newSentence != null && (lastSentence + newSentence).length() >= chunkSize) {
                            lastSentence = "";
                        }
						if (newSentence != null && newSentence.length() >= chunkSize) {
							newSentence = !sentences.isEmpty() ? sentences.removeFirst() : null;
						}
                    }
                    break;

				case HttpRequestResponseMsg msg:
					if (msg.exception == null) {
						try {
							Embedding embedding = new Embedding(
								(String) msg.tag,
									((List<Double>)new Gson().fromJson(msg.response.body().string(), LinkedList.class).getFirst())
							);
							originalRequest.getEmbeddings().add(embedding);
						}
						catch (Exception e) {
							log.error("{}: {}", self.path, e.getMessage());
						}
					} else
						log.error("Embeddings exception {}", msg.exception.getMessage());

					if (0 == --chunksLeft) {
						stop();
					}
					break;

				default: log.error("Got unexpected message $message");
			}
		};
	}

	private void requestEmbedding(String chunk) {

		Request embedRequest = HttpService.buildPostRequest(
			api,
			new Gson().toJson(Map.of("inputs", chunk), LinkedHashMap.class),
			Map.of(
				"Content-Type", "application/json",
				"Accept", "application/json"
			)
		);
		HttpRequestResponseMsg rrm = new HttpRequestResponseMsg(self, embedRequest);
		rrm.tag = chunk;
		request(rrm);
	}

	/*
	 * Always reply to sender even if we error'd somehow
	 */
	private void stop() {
		if (null != originalSender)
			originalSender.tell(originalRequest, parent);
		stopSelf();
	}

	/**
	 * Uses EmbeddingsRequestResponseMsg pass-through message
	 */
	public static class CompletableEmbeddingsRequestMsg extends CompletionRequestMsg<EmbeddingsRequestResponseMsg> {

		CompletableEmbeddingsRequestMsg(ActorRef target, CompletableFuture<EmbeddingsRequestResponseMsg> future, String text) {
			super(target, future, new EmbeddingsRequestResponseMsg(text));
		}
	}
}

