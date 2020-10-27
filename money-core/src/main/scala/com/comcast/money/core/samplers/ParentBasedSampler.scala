package com.comcast.money.core.samplers
import com.comcast.money.api.SpanId
import com.typesafe.config.Config

/**
 * A sampler that uses the parent span sampling decision if one exists, otherwise uses the root sampler
 * to determine the sampler result.
 */
final class ParentBasedSampler extends ConfigurableSampler {
  var root: Sampler = AlwaysOnSampler
  var remoteSampled: Sampler = AlwaysOnSampler
  var remoteNotSampled: Sampler = AlwaysOffSampler
  var localSampled: Sampler = AlwaysOnSampler
  var localNotSampled: Sampler = AlwaysOffSampler

  override def shouldSample(spanId: SpanId, parentSpanId: Option[SpanId], spanName: String): SamplerResult = {
    val sampler = parentSpanId match {
      case Some(id) => (id.isRemote, id.isSampled) match {
        case (true, true) => remoteSampled
        case (true, false) => remoteNotSampled
        case (false, true) => localSampled
        case (false, false) => localNotSampled
      }
      case None => root
    }
    sampler.shouldSample(spanId, parentSpanId, spanName)
  }

  override def configure(conf: Config): Unit = {
    root = SamplerFactory.create(conf.getConfig("root"))
    remoteSampled = findSampler(conf, "remote-sampled", remoteSampled)
    remoteNotSampled = findSampler(conf, "remote-not-sampled", remoteNotSampled)
    localSampled = findSampler(conf, "local-sampled", localSampled)
    localNotSampled = findSampler(conf, "local-not-sampled", localNotSampled)
  }

  def findSampler(conf: Config, name: String, default: Sampler): Sampler =
    if (conf.hasPath(name)) {
      SamplerFactory.create(conf.getConfig(name))
    } else default
}
