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


import com.mentalresonance.dust.core.actors.*
import com.mentalresonance.dust.core.actors.lib.PipelineActor
import com.mentalresonance.dust.core.actors.lib.ServiceManagerActor
import com.mentalresonance.dust.core.msgs.StartMsg
import com.mentalresonance.dust.nlp.embeddings.EmbeddingDistance
import com.mentalresonance.dust.nlp.embeddings.EmbeddingsRequestResponseMsg
import com.mentalresonance.dust.nlp.embeddings.huggingface.HFEmbeddingAPIServiceActor
import groovy.util.logging.Slf4j
import spock.lang.Specification

/**
 * Simple test of Embeddings. THis is NOT an efficient way to do things, but is here as an example of how to wire up
 * a pipeline to process text, extract embeddings from the text and match chunks of text via vector similarity testing.
 * <br/>
 * A real system would probably use a database with vector extensions (e.g. Postgres with PGVector) to store
 * embeddings and compute matching embeddings.
 * <br/>
 * This little demo pipeline consists of 4 stages:
 * <pre>
 *     1. Read a document as one entire string (something we usually would not do - better to 'pre chunk it')
 *        and then use a HFEmbeddingAPIServiceActor to generate embeddings (chunk size of 256).
 *     2. Take the EmbeddingsRequestResponseMsg from stage 1 and build a map of [ Embeddings Vector ] => Text Chunk.
 *     3. Store the document embeddings map from stage 2. Stage 3 contains a list of predefined strings as queries
 *     	  to the document. For each String:
 *     	  		* Get its embedding
 *     	  		* Run the keys (embeddings) of the map through a Cosine similarity test with the query embeddings.
 *     	  		* Chose the closest and send a pair [Query, Selected Chunk] to the pipe
 *     4. Pretty print the resulting matches.
 * </pre
 */
@Slf4j
class EmbeddingsTest extends Specification {

	static boolean worked = false

	@Slf4j
	static class BuildDataActor extends Actor {

		static Props props() { Props.create(BuildDataActor) }

		ActorBehavior createBehavior() {
			(message) -> {
				switch(message) {
					// Simply extract embeddings from msg and build a map from vectors => chunks
					case EmbeddingsRequestResponseMsg:
						EmbeddingsRequestResponseMsg msg = (EmbeddingsRequestResponseMsg)message
						Map data = [:]
						msg.embeddings.each {
							data[it.embedding] = it.chunk
						}
						parent.tell(data, self)
						break
				}
			}
		}
	}

	@Slf4j
	static class SelectChunksActor extends Actor {

		static Props props() { Props.create(SelectChunksActor) }

		Map<List<Double>, String> chunks

		List<String> questions = [
		    "Trump's response to michigan news",
			"What are the contents of the legal brief",
			"Was calling Georgia and official act or not"
		]

		/**
		 * Use a traditional Dust 'Pump with StartMsgs until nothing left to do (then in this case shut down
		 * the pipe)
		 * @return
		 */
		ActorBehavior createBehavior() {
			(message) -> {
				switch(message) {

					case Map:
						chunks = message as Map<List<Float>, String>
						tellSelf(new StartMsg())
						break

					case StartMsg:
						if (questions) {
							String question = questions.removeFirst()
							// Get embeddings for the query
							actorOf(HFEmbeddingAPIServiceActor.props('http:/192.168.1.184:8080/embed', 256))
								.tell(
									new EmbeddingsRequestResponseMsg(question),
									self
								)
						} else
							// Allow anything ahead in the pipe (logging) to finish
							scheduleIn(new PoisonPill(), 500, parent)
						break

					case EmbeddingsRequestResponseMsg:
						EmbeddingsRequestResponseMsg msg = (EmbeddingsRequestResponseMsg)message
						Double max = 0.0d
						String chunk = "Nothing found"

						chunks.each {
							Double dp = EmbeddingDistance.cosineSimilarity(msg.embeddings[0].embedding, it.key)
							if (dp > max) {
								max = dp
								chunk = it.value
							}
						}
						// Send match on
						parent.tell([msg.text, chunk], self)
						// And pump
						tellSelf(new StartMsg())
						break

					default:
						log.warn "Unexpected message $message"
				}
			}
		}
	}

	@Slf4j
	static class DisplayMatchActor extends Actor {

		static Props props() { Props.create(DisplayMatchActor) }

		// Pretty print
		ActorBehavior createBehavior() {
			(message) -> {
				switch(message) {
					case List<String>:
						log.info "\nQuery text: ${message[0]} \nResponse chunk: ... ${message[1]} ..."
						worked = true
						break
				}
			}
		}
	}

	def "Embeddings"() {
		when:
			ActorSystem system = new ActorSystem("Test")

			ActorRef pipe = system.context.actorOf(PipelineActor.props([
				ServiceManagerActor.props(HFEmbeddingAPIServiceActor.props('http:/192.168.1.184:8080/embed', 256), 1),
				BuildDataActor.props(),
				SelectChunksActor.props(),
				DisplayMatchActor.props()
			]))

			pipe.tell(new EmbeddingsRequestResponseMsg(getDocument("article2.txt")), null)
			pipe.waitForDeath()
			system.stop()
		then:
			worked
	}

	/**
	 * Read the document as one string. Typically we'd use a chunking strategy to 'gross chunk' the document then
	 * for each big chunk generate embeddings, but for this demo we take the low road.
	 * @return
	 */
	private String getDocument(String name) {
		this.getClass()
			.getClassLoader()
			.getResourceAsStream(name)
			.withReader {
				it.readLines().join('\n')
			}
	}
}