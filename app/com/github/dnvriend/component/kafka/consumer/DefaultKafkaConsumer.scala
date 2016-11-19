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

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.serialization.Serializer
import akka.stream.scaladsl.Source
import cakesolutions.kafka.akka.KafkaConsumerActor
import com.typesafe.config.Config
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

/**
 * KafkaConsumer is a Source that emits CommittableConsumerRecords.
 * Its a wrapper around the [[cakesolutions.kafka.akka.KafkaConsumerActor]] that translates its
 * protocol to akka-streams.
 *
 * The basic use case is to configure the KafkaConsumerActor, pass it a akka-serializer for your base type
 * and let it emit CommittableConsumerRecords that must be committed before the next message will be retrieved
 * from the queue.
 *
 * {{{
 * val src: Source[CommittableConsumerRecord, ActorRef]
 *
 * val result: Future[Seq[String]] =
 * src.map { record =>
 *   record.commit
 *   record.record
 * }.take(100).runWith(Sink.seq)
 * }}}
 *
 * @param kafkaClientConf The kafka consumer actor configuration
 * @param serializer      the akka-serialization serializer to use for your base type, eg. the DataMessageSerializer
 * @param system          The actor system to use
 */
class DefaultKafkaConsumer(kafkaClientConf: Config, serializer: Serializer, system: ActorSystem)
    extends KafkaConsumer {
  private def createKafkaClient(producerRef: ActorRef): ActorRef = {
    system.actorOf(
      KafkaConsumerActor.props(kafkaClientConf, new StringDeserializer, new ByteArrayDeserializer, producerRef)
    )
  }

  override def source: Source[KafkaConsumerProtocol, ActorRef] = {
    Source.actorPublisher(Props(new KafkaConsumerActorPublisher(serializer, createKafkaClient)))
  }
}
