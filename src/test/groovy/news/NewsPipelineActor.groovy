package news

import com.mentalresonance.dust.core.actors.ActorBehavior
import com.mentalresonance.dust.core.actors.Props
import com.mentalresonance.dust.core.actors.SupervisionStrategy
import com.mentalresonance.dust.core.actors.lib.LogActor
import com.mentalresonance.dust.core.actors.lib.PipelineActor
import com.mentalresonance.dust.core.actors.lib.ServiceManagerActor
import com.mentalresonance.dust.core.system.exceptions.ActorInstantiationException
import com.mentalresonance.dust.feeds.rss.RssFeedPipeActor
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

@CompileStatic
@Slf4j
class NewsPipelineActor extends PipelineActor {

	static Props props(String query, List<String> rssFeeds, String filter, List<String> entities)
	{
		List<Props> feeds = []
		List<String> names = []

		rssFeeds?.each {
			feeds << RssFeedPipeActor.props(it, 3600*1000L)
			names << it.md5()
		}

		List<Props> pipeProps = [
			PipelineFeedHubActor.props(feeds, names),
			//ServiceManagerActor.props(PageContentExtractionServiceActor.props(), 4),
			ServiceManagerActor.props(ContentFilterPipeServiceActor.props(filter), 2),
			//ServiceManagerActor.props(EntitiesExtractionServiceActor.props(entities), 2),
			LogActor.props()
		]
		List<String> pipeNames = [
			'feed',
			'content',
			'filter',
			'entities',
			'log'
		]
		Props.create(NewsPipelineActor, pipeProps, pipeNames)
	}

	NewsPipelineActor(List<Props> props, List<String> names) throws ActorInstantiationException {
		super(props, names)
	}

	@Override
	void postStop() {
		super.postStop()
		log.info "${self.path} is shutting down"
	}
	/**
	 * Make pipeline more robust with supervision strategy
	 */
	@Override
	protected void preStart() {
		supervisor = new SupervisionStrategy(SupervisionStrategy.SS_RESTART, SupervisionStrategy.MODE_ONE_FOR_ONE)
		super.preStart()
	}

	@Override
	ActorBehavior createBehavior() {
		(message) -> {
			switch(message) {
				/*
				 * Let this message be handled by the default Pipeline handler which will send it to
				 * the first stage in the Pipe - the feed hub Actor
				case AddFeedMsg:
					AddFeedMsg msg = (AddFeedMsg)message
					break
				*/
				default: super.createBehavior().onMessage(message as Serializable)
			}
		}
	}
}

