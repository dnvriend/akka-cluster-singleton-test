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

package com.github.dnvriend.component.kafka.consumer

import akka.actor.ActorRef
import cakesolutions.kafka.akka.{KafkaConsumerActor, Offsets}

trait KafkaConsumerProtocol
final case class StartBatch(offset: Offsets, length: Long) extends KafkaConsumerProtocol

object EndBatch {
  def unapply(arg: EndBatch): Option[Offsets] =
    Option(arg.offsets)
}

class EndBatch(val kafka: ActorRef, val offsets: Offsets, val batchSize: Long) extends KafkaConsumerProtocol {
  def commit: Unit = {
    kafka ! KafkaConsumerActor.Confirm(offsets, commit = true)
  }
}

object CommittableConsumerRecord {
  def unapply(arg: CommittableConsumerRecord): Option[(Any)] =
    Option(arg.record)
  def apply(kafka: ActorRef, offsets: Offsets, batchSize: Long, record: Any): CommittableConsumerRecord =
    new CommittableConsumerRecord(kafka, offsets, batchSize, record)
}

class CommittableConsumerRecord(val kafka: ActorRef, val offsets: Offsets, val batchSize: Long, val record: Any) extends KafkaConsumerProtocol {
  def commit: Unit = {
    kafka ! KafkaConsumerActor.Confirm(offsets, commit = true)
  }
}
