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
import com.mentalresonance.dust.core.msgs.Terminated
import com.mentalresonance.dust.nlp.genericgpt.GenericGptAPIServiceActor
import com.mentalresonance.dust.nlp.genericgpt.ollama.OllamaRequestResponseMsg
import groovy.util.logging.Slf4j
import spock.lang.Specification

/**
 * Simple call to a local GPT server. Supply your ChatGPT key via gpt_key environment variable.
 */
@Slf4j
class LocalGPTTest extends Specification {

	static boolean worked = false

	@Slf4j
	static class ChatActor extends Actor {

		String key = System.getenv("gpt_key")

		static Props props() { Props.create(ChatActor) }

		/**
		 * GenericGptAPIServiceActor is a Service Actor but we are only making one Ollama request. We use the
		 * fact it will stop after sending the response to trigger ChatActor to stop and the test to complete
		 */
		void preStart() {
			ActorRef gptRef = watch(actorOf(GenericGptAPIServiceActor.props('http://localhost:11434/api/generate', null)))
			gptRef.tell(new OllamaRequestResponseMsg("Can you give in depth summary of the movie 2001 A Space Odyssey"), self)
			log.info "Sent request to Ollama ...."
		}

		ActorBehavior createBehavior() {
			(message) -> {
				switch(message) {
					case OllamaRequestResponseMsg:
						OllamaRequestResponseMsg msg = (OllamaRequestResponseMsg)message
						log.info "Ollama Response: ${msg.getUtterance()}"
						worked = true
						break

					case Terminated:
						stopSelf()
						break
				}
			}
		}
	}

	def "LocalChat"() {
		when:
			ActorSystem system = new ActorSystem("Test")
			system.context.actorOf(ChatActor.props()).waitForDeath()
			system.stop()
		then:
			worked
	}
}