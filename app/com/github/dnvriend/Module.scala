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

package com.github.dnvriend

import akka.actor.{ActorSystem, PoisonPill, Props}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.util.Timeout
import com.github.dnvriend.component.foo._
import com.google.inject.{AbstractModule, Provides}
import play.api.libs.concurrent.AkkaGuiceSupport

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Module extends AbstractModule with AkkaGuiceSupport {
  override def configure() = {
    bind(classOf[FooService])
      .annotatedWith(classOf[Default])
      .to(classOf[DefaultFooService])

    bind(classOf[Timeout])
      .toInstance(Timeout(10.seconds))
  }

  @Provides
  @ClusterSingleton
  def singleFooServiceProvider(system: ActorSystem)(implicit ec: ExecutionContext, timeout: Timeout): FooService = {
    val fooServiceSingletonMgr = system.actorOf(
      ClusterSingletonManager.props(
        singletonProps = Props(classOf[FooServiceActor]),
        terminationMessage = PoisonPill,
        settings = ClusterSingletonManagerSettings(system)
      ),
      name = "foo-service"
    )

    val fooServiceSingletonProxy = system.actorOf(
      ClusterSingletonProxy.props(
        singletonManagerPath = "/user/foo-service",
        settings = ClusterSingletonProxySettings(system)
      ),
      name = "foo-service-proxy"
    )

    new ClusterSingletonFooService(fooServiceSingletonProxy)
  }
}
