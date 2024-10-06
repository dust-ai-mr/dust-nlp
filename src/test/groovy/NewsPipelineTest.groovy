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
import com.mentalresonance.dust.core.actors.lib.LogActor
import com.mentalresonance.dust.core.actors.lib.PipelineActor
import com.mentalresonance.dust.core.actors.lib.ServiceManagerActor
import com.mentalresonance.dust.core.services.FSTPersistenceService
import com.mentalresonance.dust.nlp.chatgpt.ChatGptAPIServiceActor
import news.ContentFilterPipeServiceActor
import news.PipelineFeedHubActor
import news.RssLocatorServiceActor.VerifyFeedsMsg
import groovy.util.logging.Slf4j
import news.RssLocatorServiceActor
import news.SummarizerPipeServiceActor
import spock.lang.Specification

/**
 *
 */
@Slf4j
class NewsPipelineTest extends Specification {

	static String topic = "Electric Vehicle charging"

	@Slf4j
	static class NewsActor extends Actor {

		static Props props() { Props.create(NewsActor) }

		void preStart() {
			actorOf(RssLocatorServiceActor.props()).tell(
				new RssLocatorServiceActor.RssFeedFinderMsg(topic), self
			)
		}

		ActorBehavior createBehavior() {
			(message) -> {
				switch(message) {
					case VerifyFeedsMsg:
						ActorRef newsPipe = actorOf(PipelineActor.props([
								PipelineFeedHubActor.props([], []),
								ServiceManagerActor.props(ContentFilterPipeServiceActor.props(topic, []), 1),
								ServiceManagerActor.props(SummarizerPipeServiceActor.props(), 1),
								LogActor.props()
							], [
								'feeds',
								'filter',
								'summarizer',
								'logger'
							]),
						'newspipe')

						((VerifyFeedsMsg)message).verifiedUrls.each {
							newsPipe.tell(new PipelineFeedHubActor.AddFeedMsg(it), self)
						}
						break

					default:
						log.error "??? $message"

				}
			}
		}
	}

	def "News"() {
		String key = System.getenv('gpt_key')
		when:
			ActorSystem system = new ActorSystem("Test")
			system.setPersistenceService(FSTPersistenceService.create())

			system.context.actorOf(
				ServiceManagerActor.props(ChatGptAPIServiceActor.props((ActorRef)null, key), 2),
				'chatgpt'
			)
			system.context.actorOf(NewsActor.props(), 'news')
			Thread.sleep(150000)
			system.stop()
		then:
			true
	}
}