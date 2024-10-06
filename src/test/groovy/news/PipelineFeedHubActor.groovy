package news

import com.mentalresonance.dust.core.actors.ActorBehavior
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.core.actors.SupervisionStrategy
import com.mentalresonance.dust.core.actors.lib.PipelineHubActor
import com.mentalresonance.dust.core.msgs.StartMsg
import com.mentalresonance.dust.feeds.rss.RssFeedPipeActor
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@Slf4j
@CompileStatic
class PipelineFeedHubActor extends PipelineHubActor {

	static Props props(List<Props> actors, List<String> names) {
		Props.create(PipelineFeedHubActor, actors, names)
	}

	PipelineFeedHubActor(List<Props> actors, List<String> names) {
		super(actors, names)
	}

	@Override
	void preStart() {
		supervisor = new SupervisionStrategy(SupervisionStrategy.SS_RESTART, SupervisionStrategy.MODE_ONE_FOR_ONE)
		super.preStart()
	}

	@Override
	ActorBehavior createBehavior() {
		(message) -> {
			switch(message) {
				case AddFeedMsg:
					AddFeedMsg msg = (AddFeedMsg)message
					log.info "${self.path} adding RSS feed ${msg.feedUrl}"
					actorOf(RssFeedPipeActor.props(msg.feedUrl, 3600*1000L), msg.feedUrl.md5()).tell(new StartMsg(), self)
					break

				default: super.createBehavior().onMessage(message as Serializable)
			}
		}
	}

	static class AddFeedMsg implements Serializable {
		String feedUrl

		AddFeedMsg(String url) { feedUrl = url }
	}
}
