package news

import com.mentalresonance.dust.core.actors.Actor
import com.mentalresonance.dust.core.actors.ActorBehavior
import com.mentalresonance.dust.core.actors.ActorRef
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.html.msgs.HtmlDocumentMsg
import com.mentalresonance.dust.nlp.chatgpt.ChatGptRequestResponseMsg
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class ContentFilterPipeServiceActor extends Actor {

	String filter
	String exclusions
	HtmlDocumentMsg originalMsg

	static Props props(String filter, List<String> exclusions = null) {
		Props.create(ContentFilterPipeServiceActor, filter, exclusions)
	}

	ContentFilterPipeServiceActor(String filter, List<String> exclusions) {
		this.filter = filter
		this.exclusions = exclusions?.join(' or ') ?: null
	}

	@Override
	ActorBehavior createBehavior() {
		(message) -> {
			switch(message) {
				case HtmlDocumentMsg:
					/*
					 * No filter to bother with - pass it back to the pipe
					 */
					if (! filter) {
						sender.tell(message, parent)
						context.stop(self)
					}
					else {
						originalMsg = (HtmlDocumentMsg)message
						String request =  "Does '${originalMsg.title}' refer to $filter " +
										   "${exclusions ? "but does not refer to ${exclusions}" : ''}? " +
										  "Answer simply yes or no."
						context.actorSelection('/user/chatgpt').tell(new ChatGptRequestResponseMsg(request), self)
					}
					break

				case ChatGptRequestResponseMsg:
					ChatGptRequestResponseMsg msg = (ChatGptRequestResponseMsg)message
					String response = msg.getUtterance()?.toLowerCase()

					if (response?.toLowerCase()?.trim()?.startsWith('yes')) {
						log.trace "**** ${originalMsg.uuid} ${originalMsg.title} PASSES filter $filter"
						/*
						 * The actual pipe member is my parent (the service manager) and the originalSender
						 * is the pipe, so send response back to the pipe as though from my parent
						 */
						grandParent.tell(originalMsg, parent)
					}
					else
						log.trace "**** ${originalMsg.uuid} ${originalMsg.title} FAILS filter $filter"
					stopSelf()
					break

				default:
					log.info "${self.path} got unexpected message $message"
			}
		}
	}
}
