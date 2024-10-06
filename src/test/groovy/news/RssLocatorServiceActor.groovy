package news

import com.mentalresonance.dust.core.actors.Actor
import com.mentalresonance.dust.core.actors.ActorBehavior
import com.mentalresonance.dust.core.actors.ActorRef
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.http.service.HttpRequestResponseMsg
import com.mentalresonance.dust.http.trait.HttpClientActor
import com.mentalresonance.dust.nlp.ChatUtils
import com.mentalresonance.dust.nlp.chatgpt.ChatGptRequestResponseMsg
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

/**
 * We ask GPT for RSS urls which it can sometimes find. It may hallucinate or the feed may have gone away
 * so we have to check it.
 */
@Slf4j
@CompileStatic
class RssLocatorServiceActor extends Actor implements HttpClientActor {

	VerifyFeedsMsg verifyFeedsMsg
	ActorRef originalSender

	static Props props() {
		Props.create(RssLocatorServiceActor)
	}

	@Override
	ActorBehavior createBehavior() {
		(message) -> {
			switch(message) {
				case RssFeedFinderMsg:
					RssFeedFinderMsg msg = (RssFeedFinderMsg)message
					originalSender = sender
					context.actorSelection('/user/chatgpt').tell(
						new RssFeedRequestMsg(msg.query),
						self
					)
					break

				case RssFeedRequestMsg:
					RssFeedRequestMsg msg = (RssFeedRequestMsg)message
					List<String> urls = ChatUtils.numericList(msg.utterance)
					self.tell(new VerifyFeedsMsg(urls), self)
					break

				case VerifyFeedsMsg:
					verifyFeedsMsg = (VerifyFeedsMsg)message
					if (verifyFeedsMsg.urls.isEmpty()) {
						originalSender?.tell(verifyFeedsMsg, null)
						context.stop(self)
					} else
						try {
							request(verifyFeedsMsg.urls.removeFirst())
						} catch (Exception e) {
							log.warn e.message
						}
					break

				/*
				 * Verify the feed. Does the page exist and is it a feed ?? ChatGPT fails in both directions
				 * Simply try to parse result as a feed
				 */
				case HttpRequestResponseMsg:
					HttpRequestResponseMsg msg = (HttpRequestResponseMsg)message

					if (null == msg.exception && msg.response.successful) {
						String url = msg.request.url().toString()
						try {
							new SyndFeedInput().build(new XmlReader(msg.response.body().byteStream()))
							verifyFeedsMsg.verifiedUrls << url
							log.info "$url is a feed !!"
						} catch (Exception e) {
							log.warn "$url exists but is not an RSS feed!"
						}
					}
					self.tell(verifyFeedsMsg, self)
					break


				default: super.createBehavior().onMessage(message as Serializable)
			}
		}
	}

	static class RssFeedFinderMsg implements Serializable {
		String query

		RssFeedFinderMsg(String query) {
			this.query = query
		}
	}

	static class RssFeedRequestMsg extends ChatGptRequestResponseMsg {
		RssFeedRequestMsg(String request) {
			super(
				"""Consider the following topic and give me a numerical list consisting *only* of urls for RSS feeds that 
				   might contain information about the topic: '$request'.

					Try hard to find real RSS urls. Each entry should consist only of the URL - nothing else. Include no descriptive text.
				""",
				DEFAULT_SYSTEM_PROMPT,
				GPT_4o)
		}
	}

	static class VerifyFeedsMsg implements Serializable {
		public List<String> urls
		public List<String> verifiedUrls = []

		VerifyFeedsMsg(List<String> urls) {
			this.urls = urls
		}

		@Override
		String toString() {
			"Verified feeds:\nverifiedUrls.join('\n')"
		}
	}
}
