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
import com.mentalresonance.dust.nlp.chatgpt.ChatGptAPIServiceActor
import com.mentalresonance.dust.nlp.chatgpt.ChatGptRequestResponseMsg
import com.mentalresonance.dust.nlp.chatgpt.ResponseGptAPIServiceActor
import com.mentalresonance.dust.nlp.chatgpt.ResponsesGptRequestResponseMsg
import groovy.util.logging.Slf4j
import spock.lang.Specification

/**
 * Simple call to ChatGPT. Supply your ChatGPT key via gpt_key environment variable.
 */
@Slf4j
class ResponseGPTTest extends Specification {

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
			ActorRef gptRef = watch(actorOf(ResponseGptAPIServiceActor.props((ActorRef)null, key)))
			gptRef.tell(new ResponsesGptRequestResponseMsg(
				"What is the current stock price for IBM. Give the date and time as well as value",
				"gpt-4o-mini"
				), self)
			log.info "Sent request to ChatGPT ...."
		}

		ActorBehavior createBehavior() {
			(message) -> {
				switch(message) {
					case ResponsesGptRequestResponseMsg:
						ResponsesGptRequestResponseMsg msg = (ResponsesGptRequestResponseMsg)message
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