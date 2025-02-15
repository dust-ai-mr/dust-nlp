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
import com.mentalresonance.dust.core.msgs.StartMsg;
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
	EmbeddingsRequestResponseMsg originalMsg;
	ActorRef originalSender;
	String api;
	LinkedList<String> sentences;
	/**
	 * Chunking. lastSentence is last sentence of current chunk. Becomes first sentence of next chunk i.e.
	 * chunks have a one sentence overlap. nextSentence will follow this overlap in the next chunk.
	 */
	String lastSentence = "" /* Of current chunk*/, nextSentence = "";

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
		dieIn(5 * 60 * 1000L);
	}

	public void postStop() {
		log.trace("{} stopped", self.path);
	}

	@Override
	public ActorBehavior createBehavior() {
		return (Serializable message) -> {
			switch(message) {
				case EmbeddingsRequestResponseMsg msg:

					originalSender = sender;
					originalMsg = msg;

					/*
					 * Chunk by sentences with a one sentence overlap between consecutive chunks
					 */

					String allText = originalMsg.getText();
					if (allText == null || allText.isEmpty()) {
						originalMsg.setEmbeddings(new LinkedList<>());
						log.warn("{} received trivial text", self.path);
						originalSender.tell(originalMsg, self);
						stopSelf();
					} else {
						if (allText.length() <= chunkSize) {
							sentences = new LinkedList<>(List.of(allText));
						} else
							sentences = Words.sentences(originalMsg.getText(), Locale.ENGLISH);
						tellSelf(new StartMsg());
					}
					break;

				case StartMsg ignored: // Get and process next chunk

					if (sentences.isEmpty()) {
						originalSender.tell(originalMsg, self);
						stopSelf();
					}
					else {
						// Starting a new chunk - start with end of last chunk
						String text = lastSentence;
						int currentSize = text.length();
						nextSentence = sentences.removeFirst();

						// Build the chunk
						while (nextSentence != null && (currentSize = currentSize + nextSentence.length()) <= chunkSize) {
							text = text + nextSentence;
							lastSentence = nextSentence;
							nextSentence = !sentences.isEmpty() ? sentences.removeFirst() : null;
						}
						/* If lastSentence + nextSentence exceeds chunk size we will just spin on lastSentence
						   So fix things up by truncating nextSentence appropriately.
						 */
						if (null != nextSentence) {
							if ( (lastSentence + nextSentence).length() > chunkSize) {
								int l = chunkSize - lastSentence.length();
								nextSentence = nextSentence.substring(0, l);
								log.trace("{} adjusted chunk - size is {}", self.path, (lastSentence + nextSentence).length());
							}
						}
						if (!text.isEmpty()) {
							requestEmbedding(text);
						} else {
							// First sentence of chunk was already too long
							tellSelf(new StartMsg());
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
							originalMsg.getEmbeddings().add(embedding);
						}
						catch (Exception e) {
							log.error("{}: {}", self.path, e.getMessage());
						}
					} else
						log.error("Embeddings exception {}", msg.exception.getMessage());

					tellSelf(new StartMsg());
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
			originalSender.tell(originalMsg, parent);
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

