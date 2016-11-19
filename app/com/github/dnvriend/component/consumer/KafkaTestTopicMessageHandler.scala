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

import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.{KillSwitches, Materializer, UniqueKillSwitch}
import com.github.dnvriend.component.foo.FooService
import com.github.dnvriend.component.kafka.consumer.{CommittableConsumerRecord, EndBatch, KafkaConsumerProtocol}
import com.github.dnvriend.component.model.Person

import scala.concurrent.{ExecutionContext, Future}

object KafkaTestTopicMessageHandler {
  def apply(kafkaEvents: Source[KafkaConsumerProtocol, _], fooService: FooService, logger: Logger)(implicit ec: ExecutionContext, mat: Materializer): UniqueKillSwitch =
    kafkaEvents
      .viaMat(KillSwitches.single)(Keep.right)
      .mapAsync(1) {

        case msg @ CommittableConsumerRecord(Person(name, age)) =>
          for {
            fooMessage <- fooService.foo(name, age)
            _ = println(s"==> [Singleton - KafkaTestTopicMessageHandler] ==> from fooService: $fooMessage")
            _ = println(s"==> [Singleton - KafkaTestTopicMessageHandler] ==> ${msg.record}, ${msg.offsets}")
          } yield msg

        case e =>
          Future.successful(e)
      }.toMat(Sink.foreach {
        case end: EndBatch =>
          end.commit
          println(s"==> [Singleton - KafkaTestTopicMessageHandler] ==> batchSize: ${end.batchSize}")
        case msg =>
      })(Keep.left).run()
}
