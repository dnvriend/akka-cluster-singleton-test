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

final case class CommittableConsumerRecord(kafka: ActorRef, offsets: Offsets, record: AnyRef) {
  def commit: Unit = {
    kafka ! KafkaConsumerActor.Confirm(offsets, commit = true)
  }
}
