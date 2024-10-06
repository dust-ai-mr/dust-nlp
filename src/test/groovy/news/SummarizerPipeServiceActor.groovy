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

package news

import com.mentalresonance.dust.core.actors.Actor
import com.mentalresonance.dust.core.actors.ActorBehavior
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.html.msgs.HtmlDocumentMsg
import com.mentalresonance.dust.nlp.chatgpt.ChatGptRequestResponseMsg
import groovy.util.logging.Slf4j

@Slf4j
class SummarizerPipeServiceActor extends Actor {

	static Props props() { Props.create(SummarizerPipeServiceActor) }

	HtmlDocumentMsg originalMessage

	ActorBehavior createBehavior() {
		(message) -> {
			switch(message) {
				case HtmlDocumentMsg:
					originalMessage = (HtmlDocumentMsg)message
					String prompt =
"""You are a competent reporter. Give a one paragraph summary of the following. Give me only the summary.

Text: ${originalMessage.extractContent()}
"""
					context.actorSelection('/user/chatgpt').tell(
						new ChatGptRequestResponseMsg(prompt),
						self
					)
					break

				case ChatGptRequestResponseMsg:
					ChatGptRequestResponseMsg msg = (ChatGptRequestResponseMsg)message
					if (msg.response) {
						String summary = msg.utterance
						grandParent.tell("\nTitle: ${originalMessage.title}\n\nSummary: $summary\n", parent)
					} else
						log.warn "No response to ${msg.request}"
					stopSelf()
					break
			}
		}
	}
}
