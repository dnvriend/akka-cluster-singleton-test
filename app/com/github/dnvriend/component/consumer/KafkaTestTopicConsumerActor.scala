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

package com.github.dnvriend.component.consumer

import java.util.logging.Logger

import akka.actor.{Actor, ActorSystem}
import akka.stream.{Materializer, UniqueKillSwitch}
import com.github.dnvriend.component.foo.FooService
import com.github.dnvriend.component.kafka.consumer.DefaultKafkaConsumer
import com.github.dnvriend.component.serializer.PersonSerializer
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext

object KafkaTestTopicConsumerActor {
  case object Terminate
}

class KafkaTestTopicConsumerActor(fooService: FooService, serializer: PersonSerializer, kafkaConfig: Config, logger: Logger)(implicit system: ActorSystem, ec: ExecutionContext, mat: Materializer) extends Actor {
  println("===> Launching: KafkaTestTopicConsumerActor for cluster singleton")
  val killSwitch: UniqueKillSwitch =
    KafkaTestTopicMessageHandler(new DefaultKafkaConsumer(kafkaConfig, serializer, system).source, fooService, logger)

  override def receive: Receive = {
    case KafkaTestTopicConsumerActor.Terminate =>
      killSwitch.shutdown()
      context.stop(self)
  }
}
