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

import javax.inject.Inject

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.serialization.Serializer
import akka.stream.Materializer
import akka.util.Timeout
import cakesolutions.kafka.KafkaProducer.Conf
import com.github.dnvriend.component.foo._
import com.github.dnvriend.component.kafka.producer.{DefaultKafkaProducer, KafkaProducer}
import com.google.inject.{AbstractModule, Provider}
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import play.api.Configuration
import cakesolutions.kafka.{KafkaConsumer => CakeSolutionsKafkaConsumer, KafkaProducer => CakeSolutionsKafkaProducer}
import com.github.dnvriend.component.kafka.annotation.TestTopic
import com.github.dnvriend.component.kafka.consumer.{DefaultKafkaConsumer, KafkaConsumer}
import com.github.dnvriend.component.serializer.PersonSerializer
import com.github.dnvriend.component.testtopicconsumer.TestTopicConsumer
import play.api.libs.concurrent.AkkaGuiceSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bind(classOf[FooService])
      .annotatedWith(classOf[Default])
      .to(classOf[DefaultFooService])

    bind(classOf[FooService])
      .annotatedWith(classOf[ClusterSingleton])
      .toProvider(classOf[ClusterSingletonFooProvider])
      .asEagerSingleton()

    bind(classOf[KafkaProducer])
      .annotatedWith(classOf[TestTopic])
      .toProvider(classOf[TestTopicKafkaProducerProvider])

    bind(classOf[KafkaConsumer])
      .annotatedWith(classOf[TestTopic])
      .toProvider(classOf[TestTopicKafkaConsumerProvider])

    bind(classOf[Serializer])
      .to(classOf[PersonSerializer])

    bind(classOf[TestTopicConsumer])
      .asEagerSingleton()

    bind(classOf[Timeout])
      .toInstance(Timeout(10.seconds))
  }
}

class TestTopicKafkaProducerProvider @Inject() (config: Configuration) extends Provider[KafkaProducer] {
  override def get(): KafkaProducer = {
    val cfg = config.underlying.getConfig("kafka-test")
    new DefaultKafkaProducer(CakeSolutionsKafkaProducer[String, Array[Byte]](Conf(cfg, new StringSerializer(), new ByteArraySerializer())))
  }
}

class TestTopicKafkaConsumerProvider @Inject() (config: Configuration, personSerializer: PersonSerializer)(implicit system: ActorSystem) extends Provider[KafkaConsumer] {
  override def get(): KafkaConsumer = {
    val kafkaConfig = config.underlying.getConfig("kafka-test")
    new DefaultKafkaConsumer(kafkaConfig, personSerializer, system)
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

