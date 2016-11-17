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

package com.github.dnvriend.component.serializer

import akka.serialization.Serializer
import com.github.dnvriend.component.model.Person
import play.api.libs.json.{Format, Json}

class PersonSerializer extends Serializer {
  override def identifier: Int = Int.MaxValue
  override def includeManifest: Boolean = false

  def serialize[A: Format](a: A): Array[Byte] =
    Json.stringify(Json.toJson(a)).getBytes("UTF-8")

  override def toBinary(obj: AnyRef): Array[Byte] = obj match {
    case msg: Person => serialize(msg)
  }

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    Json.parse(bytes).as[Person]
  }
}
