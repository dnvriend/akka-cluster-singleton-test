/*
 * Copyright 2016 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend

import java.util.logging.Logger

import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.serialization.Serializer
import akka.stream.Materializer
import akka.util.Timeout
import cakesolutions.kafka.KafkaProducer.Conf
import cakesolutions.kafka.{KafkaConsumer => CakeSolutionsKafkaConsumer, KafkaProducer => CakeSolutionsKafkaProducer}
import com.github.dnvriend.component.consumer.{KafkaTestTopicConsumerActor, TestTopicMessageHandler}
import com.github.dnvriend.component.foo._
import com.github.dnvriend.component.kafka.annotation.TestTopic
import com.github.dnvriend.component.kafka.consumer.{DefaultKafkaConsumer, KafkaConsumer}
import com.github.dnvriend.component.kafka.producer.{DefaultKafkaProducer, KafkaProducer}
import com.github.dnvriend.component.serializer.PersonSerializer
import com.google.inject.{AbstractModule, Inject, Provider, Provides}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import play.api.Configuration
import play.api.libs.concurrent.AkkaGuiceSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    // person serializer
    bind(classOf[Serializer])
      .to(classOf[PersonSerializer])

    // akka.util.Timeout for ask pattern
    bind(classOf[Timeout])
      .toInstance(Timeout(10.seconds))

    // foo service
    bind(classOf[FooService])
      .annotatedWith(classOf[Default])
      .to(classOf[DefaultFooService])

    // foo service singleton
    bind(classOf[FooService])
      .annotatedWith(classOf[ClusterSingleton])
      .toProvider(classOf[ClusterSingletonFooProvider])
      .asEagerSingleton()

    // kafka producer to topic: test
    bind(classOf[KafkaProducer])
      .annotatedWith(classOf[TestTopic])
      .toProvider(classOf[TestTopicKafkaProducerProvider])
      .asEagerSingleton()

    // kafka message handler that consumes topic: test
    bind(classOf[TestTopicMessageHandler])
      .toProvider(classOf[TestTopicMessageHandlerProvider])
      .asEagerSingleton()

    // kafka consumer of topic: test
    bind(classOf[ActorRef])
      .toProvider(classOf[TestTopicKafkaConsumerClusterSingletonProvider])
      .asEagerSingleton
  }
}

class TestTopicKafkaProducerProvider @Inject() (config: Configuration) extends Provider[KafkaProducer] {
  override def get(): KafkaProducer = {
    val cfg = config.underlying.getConfig("kafka-test")
    new DefaultKafkaProducer(CakeSolutionsKafkaProducer[String, Array[Byte]](Conf(cfg, new StringSerializer(), new ByteArraySerializer())))
  }
}

class TestTopicMessageHandlerProvider @Inject() (config: Configuration, personSerializer: PersonSerializer, logger: Logger)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) extends Provider[TestTopicMessageHandler] {
  override def get(): TestTopicMessageHandler = {
    val kafkaConfig = config.underlying.getConfig("kafka-test")
    new TestTopicMessageHandler(new DefaultKafkaConsumer(kafkaConfig, personSerializer, system), logger)
  }
}

class TestTopicKafkaConsumerClusterSingletonProvider @Inject() (@ClusterSingleton fooService: FooService, config: Configuration, serializer: PersonSerializer, logger: Logger)(implicit system: ActorSystem, ec: ExecutionContext, mat: Materializer) extends Provider[ActorRef] {
  override def get(): ActorRef = {
    val kafkaConfig = config.underlying.getConfig("kafka-test-singleton")
    val clusterSingletonManagerSettings = ClusterSingletonManagerSettings(kafkaConfig.getConfig("singleton"))
    val clusterSingletonProxySettings = ClusterSingletonProxySettings(kafkaConfig.getConfig("singleton-proxy"))
    val actorName = "singleton-kafka-test-manager"
    system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[KafkaTestTopicConsumerActor], fooService, serializer, kafkaConfig, logger, system, ec, mat),
        terminationMessage = KafkaTestTopicConsumerActor.Terminate,
        settings = clusterSingletonManagerSettings
      ),
      name = actorName
    )
    system.actorOf(ClusterSingletonProxy.props(s"/user/$actorName", clusterSingletonProxySettings), "singleton-kafka-test-proxy")
  }
}

class ClusterSingletonFooProvider @Inject() (system: ActorSystem)(implicit ec: ExecutionContext, mat: Materializer, timeout: Timeout) extends Provider[FooService] {
  override def get(): FooService = {
    val fooServiceSingletonMgr = system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[FooServiceActor], ec, mat),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system)
      ),
      name = "foo-service"
    )

    val fooServiceSingletonProxy = system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = "/user/foo-service",
        settings = ClusterSingletonProxySettings(system)
      ),
      name = "foo-service-proxy"
    )

    new ClusterSingletonFooService(fooServiceSingletonProxy)
  }
}

