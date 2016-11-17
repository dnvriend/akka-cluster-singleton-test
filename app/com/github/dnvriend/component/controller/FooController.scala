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

package com.github.dnvriend.component.controller

import javax.inject.Inject

import com.github.dnvriend.component.foo.{ClusterSingleton, Default, FooService}
import com.github.dnvriend.component.kafka.producer.PersonProducer
import com.github.dnvriend.component.model.Person
import play.api.mvc.{Action, Controller}

import scala.concurrent.ExecutionContext

class FooController @Inject() (@Default fooService: FooService, @ClusterSingleton singleFooService: FooService, personProducer: PersonProducer)(implicit ec: ExecutionContext) extends Controller {
  def getFoo(name: String, age: Int) = Action.async {
    fooService.foo(name, age).map { msg =>
      Ok(s"Your name is: $name and you are $age old, foo's message is: $msg")
    }
  }

  def getSingleFoo(name: String, age: Int) = Action.async {
    singleFooService.foo(name, age).map { msg =>
      Ok(s"Your name is: $name and you are $age old, foo's message is: $msg")
    }
  }

  def publishPerson(name: String, age: Int) = Action.async {
    personProducer.sendPerson(Person(name, age)).map { data =>
      Ok(s"Message has been sent: $data")
    }
  }
}
