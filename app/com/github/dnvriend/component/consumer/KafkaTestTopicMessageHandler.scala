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
import com.github.dnvriend.component.kafka.consumer.CommittableConsumerRecord
import com.github.dnvriend.component.model.Person

import scala.concurrent.ExecutionContext

object KafkaTestTopicMessageHandler {
  def apply(kafkaEvents: Source[CommittableConsumerRecord, _], fooService: FooService, logger: Logger)(implicit ec: ExecutionContext, mat: Materializer): UniqueKillSwitch =
    kafkaEvents
      .viaMat(KillSwitches.single)(Keep.right)
      .mapAsync(1) {
        case msg @ CommittableConsumerRecord(_, offsets, p @ Person(name, age)) =>
          println(s"==> [KafkaTestTopicMessageHandler] ==> $p, $offsets")
          fooService.foo(name, age)
            .map(msg => println(s"==> [KafkaTestTopicMessageHandler] ==> from fooService: $msg"))
            .map(_ => msg)
      }
      .toMat(Sink.foreach(_.commit))(Keep.left).run()
}
