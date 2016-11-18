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

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import com.github.dnvriend.component.kafka.consumer.{CommittableConsumerRecord, KafkaConsumer}
import com.github.dnvriend.component.model.Person

import scala.concurrent.ExecutionContext

class TestTopicConsumer(kafkaConsumer: KafkaConsumer, logger: Logger)(implicit ec: ExecutionContext, system: ActorSystem, mat: Materializer) {
  kafkaConsumer.source.map {
    case msg @ CommittableConsumerRecord(_, offsets, p @ Person(name, age)) =>
      println(s"==> [TestTopicConsumer] ==> $p, $offsets")
      msg.commit
    case msg =>
      logger.warning("==> Dropping: " + msg)
  }.runWith(Sink.ignore)
}
