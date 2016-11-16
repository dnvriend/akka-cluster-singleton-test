# akka-cluster-singleton-test
A small study project on akka-cluster-singleton.

## Description
Project uses the playframework as a quick way to wire components and controllers together using
the Guice DI framework.

The project contains a single `FooService` with two implementations, a `DefaultFooService` that can
be referred to with the `@Default` annotation and a `ClusterSingletonFooService` that can be referred to
with the `@ClusterSingleton` annotation.

## Usage
A message can be sent to both services using REST eg:

__DefaultFooService:__
```bash
$ http :9000/foo/dennis/42
HTTP/1.1 200 OK
Content-Length: 75
Content-Type: text/plain; charset=utf-8
Date: Wed, 16 Nov 2016 17:00:36 GMT

Your name is: dennis and you are 42 old, foo's message is: Foo - dennis, 42
```

__ClusterSingleton:__
```bash
$ http :9000/singlefoo/dennis/42
HTTP/1.1 200 OK
Content-Length: 84
Content-Type: text/plain; charset=utf-8
Date: Wed, 16 Nov 2016 17:01:21 GMT

Your name is: dennis and you are 42 old, foo's message is: FooSingleton - dennis, 42
```

Have fun!