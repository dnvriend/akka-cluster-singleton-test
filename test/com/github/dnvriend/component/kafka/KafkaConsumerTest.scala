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

package com.github.dnvriend.component.kafka

import cakesolutions.kafka.testkit.KafkaServer
import com.github.dnvriend.TestSpec
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}

class KafkaConsumerTest extends TestSpec {
  def withKafka(f: KafkaServer => Unit): Unit = {
    val kafka: KafkaServer = new KafkaServer()
    kafka.startup()
    try f(kafka) finally kafka.close()
  }

  it should "" in withKafka { kafka =>
    kafka.produce[String, String]("foo", List(new ProducerRecord("foo", "foo")), new StringSerializer, new StringSerializer)
    1 shouldBe 1
    val result = kafka.consume[String, String]("foo", 1, 1000, new StringDeserializer, new StringDeserializer)
    println(result)
  }
}
