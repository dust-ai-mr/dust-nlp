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

import com.mentalresonance.dust.core.actors.Actor
import com.mentalresonance.dust.core.actors.ActorBehavior
import com.mentalresonance.dust.core.actors.ActorRef
import com.mentalresonance.dust.core.actors.ActorSystem
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.core.msgs.Terminated
import com.mentalresonance.dust.nlp.chatgpt.ChatGptRequestResponseMsg
import groovy.util.logging.Slf4j
import spock.lang.Specification
import com.mentalresonance.dust.nlp.chatgpt.ChatGptAPIServiceActor

/**
 * Simple call to ChatGPT. Supply your ChatGPT key via gpt_key environment variable.
 */
@Slf4j
class ChatGPTTest extends Specification {

	static boolean worked = false

	@Slf4j
	static class ChatActor extends Actor {

		String key = System.getenv("gpt_key")

		static Props props() { Props.create(ChatActor) }

		/**
		 * ChatGptAPIServiceActor is a Service Actor but we are only making one ChatGPT request. We use the
		 * fact it will stop after sending the response to trigger ChatActor to stop and the test to complete
		 */
		void preStart() {
			ActorRef gptRef = watch(actorOf(ChatGptAPIServiceActor.props((ActorRef)null, key)))
			gptRef.tell(new ChatGptRequestResponseMsg("Can you give in depth summary of the movie 2001 A Space Odyssey"), self)
			log.info "Sent request to ChatGPT ...."
		}

		ActorBehavior createBehavior() {
			(message) -> {
				switch(message) {
					case ChatGptRequestResponseMsg:
						ChatGptRequestResponseMsg msg = (ChatGptRequestResponseMsg)message
						log.info "ChatGPT Response: ${msg.getUtterance()}"
						worked = true
						break

					case Terminated:
						stopSelf()
						break
				}
			}
		}
	}

	def "CHAT"() {
		when:
			ActorSystem system = new ActorSystem("Test")
			system.context.actorOf(ChatActor.props()).waitForDeath()
			system.stop()
		then:
			worked
	}
}