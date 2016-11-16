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

package com.github.dnvriend.component.foo

import akka.actor.Actor
import akka.stream.Materializer
import com.github.dnvriend.component.foo.FooServiceActor.GetMessage
import akka.pattern.pipe
import akka.stream.scaladsl.Source

import scala.concurrent.ExecutionContext
import scalaz._
import Scalaz._

object FooServiceActor {
  final case class GetMessage(name: String, age: Int)
}

class FooServiceActor(implicit ec: ExecutionContext, mat: Materializer) extends Actor {
  // we are appending strings so Monoid[String]
  final val StringMonoid = Monoid[String]

  override def receive: Receive = {
    case GetMessage(name, age) =>
      Source.single("FooSingleton")
        .map(msg => s"$msg - $name, $age")
        .map(msg => s"$msg - ${java.util.UUID.randomUUID()}")
        .runFold(StringMonoid.zero)(StringMonoid.append(_, _))
        .pipeTo(sender())
  }
}
