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

import akka.actor.{ActorLogging, ActorRef, PoisonPill}
import akka.serialization.Serializer
import akka.stream.actor.ActorPublisher
import akka.stream.actor.ActorPublisherMessage.{Cancel, Request}
import cakesolutions.kafka.akka.{ConsumerRecords, Extractor, KafkaConsumerActor}
import com.github.dnvriend.component.kafka.consumer.serializer.UnsupportedMessage

import scalaz.Scalaz._

/**
 * A Reactive Streams compatible Publisher that will emit CommittableConsumerRecords that can be committed eg. in the
 * Sink.foreach(_.commit)
 *
 * @param serializer       the serializer to use for deserializing the base types
 * @param initKafka        a callback function that will be applied with the ActorRef of the KafkaConsumerActorPublisher, and should return the ActorRef of the KafkaActorConsumer
 */
class KafkaConsumerActorPublisher(
    serializer: Serializer,
    initKafka: ActorRef => ActorRef
) extends ActorPublisher[CommittableConsumerRecord] with ActorLogging {
  var buf: Option[CommittableConsumerRecord] = none[CommittableConsumerRecord]
  val extractor: Extractor[Any, ConsumerRecords[String, Array[Byte]]] =
    ConsumerRecords.extractor[String, Array[Byte]]
  val kafkaConsumer: ActorRef = initKafka(self)

  def automaticallyCommit(xs: Seq[Any]): Boolean = xs.exists {
    case x: Array[Byte]     => true
    case UnsupportedMessage => true
    case _                  => false
  }

  override def receive: Receive = {
    case extractor(records) =>
      val xs: Seq[AnyRef] = records.values.map(serializer.fromBinary)
      log.debug("Received: {}", xs)
      if (xs.isEmpty || automaticallyCommit(xs)) {
        log.debug("Acking message: empty: {} or autoCommit: {}", xs.isEmpty, automaticallyCommit(xs))
        kafkaConsumer ! KafkaConsumerActor.Confirm(records.offsets, commit = true)
      } else {
        buf = CommittableConsumerRecord(kafkaConsumer, records.offsets, xs.head).some
        deliverBuf()
      }
    case req: Request =>
      deliverBuf()
    case Cancel =>
      unsubscribe()
      stop()
    case msg =>
      log.debug("Dropping: {}", msg)
  }

  def subscribe(): Unit = {
    kafkaConsumer ! KafkaConsumerActor.Subscribe()
  }

  def unsubscribe(): Unit = {
    kafkaConsumer ! KafkaConsumerActor.Unsubscribe
  }

  def stop(): Unit = {
    kafkaConsumer ! PoisonPill
    context.stop(self)
  }

  override def preStart(): Unit = {
    super.preStart()
    subscribe()
  }

  override def postStop(): Unit = {
    unsubscribe()
    super.postStop()
  }

  final def deliverBuf(): Unit =
    if (totalDemand > 0) {
      log.debug("==> deliverBuffer: demand: {}, buf: {}", totalDemand, buf)
      buf.foreach(onNext)
      buf = none[CommittableConsumerRecord]
    }
}
