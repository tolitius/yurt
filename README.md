# yurt

  module  |  branch  |  status
----------|----------|----------
   yurt   | `master` | [![Circle CI](https://circleci.com/gh/tolitius/yurt/tree/master.png?style=svg)](https://circleci.com/gh/tolitius/yurt/tree/master)

[![Clojars Project](http://clojars.org/yurt/latest-version.svg)](http://clojars.org/yurt)

> <img src="doc/img/slack-icon.png" width="30px"> _any_ questions or feedback: [`#mount`](https://clojurians.slack.com/messages/mount/) clojurians slack channel (or just [open an issue](https://github.com/tolitius/yurt/issues))

- [What is it for?](#what-is-it-for)
- [Building Yurts](#building-yurts)
- [Destroying Yurts](#destroying-yurts)
- [Swapping Alternate Implementations](#swapping-alternate-implementations)
- [Building Smaller Yurts](#building-smaller-yurts)
- [Declarative Yurts](#declarative-yurts)
- [Stop functions](#stop-functions)
- [Show me](#show-me)

## What is it for?

This library manages application stateful components by packaging them in isolated local containers. It calls these containers Yurts and makes sure each Yurt has its own state and lifecyle.

This is useful to

* develop and run tests in the same REPL: by creating `dev` and `test` Yurts and work with them simultaneously
* safely run tests in parallel without a fear of shared resources: i.e. different DB schemas, files, etc.
* anything else that requires multiple clones of an application or its parts

Yurt relies on [mount](https://github.com/tolitius/mount) to build these standalone containers. It only uses mount to _discover_ component (`defstate`s), learn about components' dependecies and their start and stop functions. When the Yurt is created it is fully _detached_ from Clojure vars: it becomes a standalone map of components.

Multiple brand new _local_ Yurts with components can be created and passed down to the application / REPL to be used _simultaneously_ in the same Clojure runtime for fun and profit.

## Building Yurts

Besides adding Yurt as a project dependency (boot / lein), nothing else needs to be done to an existing mount application to build Yurts for it.

Before building local Yurts based on a mount application a "blueprint" needs to be created. Blueprint is data _about_ components that were discovered, i.e. not the actual components:

```clojure
dev=> (yurt/blueprint)
{:components
 {"neo.conf/config" {:status :not-started},
  "neo.db/db" {:status :not-started},
  "neo.www/neo-app" {:status :not-started},
  "neo.app/nrepl" {:status :not-started}},
 :blueprint
 {"neo.conf/config" {:order 1},
  "neo.db/db" {:order 2},
  "neo.www/neo-app" {:order 3},
  "neo.app/nrepl" {:order 4}}}
```

Since Yurt builds upon the knowledge that mount has about an application, this blueprint merely reflects that knowledge.

Now, based on this blueprint, we can build a local Yurt that would have real, "started" components:

```clojure
dev=> (def bp (yurt/blueprint))
dev=> (def dev-yurt (yurt/build bp))
```

A `dev-yurt` is built and ready to roll:

```clojure
dev=> dev-yurt
{:components
 {"neo.conf/config"
  {:datomic {:uri "datomic:mem://yurt"},
   :www {:port 4242},
   :nrepl {:host "0.0.0.0", :port 7878}},
  "neo.db/db"
  {:conn
   #object[datomic.peer.LocalConnection 0x3f66af9c "datomic.peer.LocalConnection@3f66af9c"],
   :uri "datomic:mem://yurt"},
  "neo.www/neo-app"
  #object[org.eclipse.jetty.server.Server 0x2dd20d61 "org.eclipse.jetty.server.Server@2dd20d61"],
  "neo.app/nrepl"
  #clojure.tools.nrepl.server.Server{:server-socket #object[java.net.ServerSocket 0x3ebc5516 "ServerSocket[addr=/0.0.0.0,localport=7878]"], :port 7878, :open-transports #object[clojure.lang.Atom 0x7026e6db {:status :ready, :val #{}}], :transport #object[clojure.tools.nrepl.transport$bencode 0x38a2d586 "clojure.tools.nrepl.transport$bencode@38a2d586"], :greeting nil, :handler #object[clojure.tools.nrepl.middleware$wrap_conj_descriptor$fn__1707 0x3c114a1 "clojure.tools.nrepl.middleware$wrap_conj_descriptor$fn__1707@3c114a1"], :ss #object[java.net.ServerSocket 0x3ebc5516 "ServerSocket[addr=/0.0.0.0,localport=7878]"]}},
 :blueprint
 {"neo.conf/config" {:order 1},
  "neo.db/db" {:order 2},
  "neo.www/neo-app" {:order 3},
  "neo.app/nrepl" {:order 4}}}
```

## Destroying Yurts

```clojure
(yurt/destroy dev-yurt)
```

will stop all the Yurt components using their `:stop` functions defined in `defstate`s.

## Swapping Alternate Implementations

While having a development Yurt in the REPL, it might be useful to create a test Yurt _in the same REPL_ to run tests against it while developing.

Usually a test Yurt would be started with a different configuration so, for example, dev and test HTTP server components can run simultaneously on different ports.

This can be done with the `:swap` option:

```clojure
(require '[clojure.edn :as edn])
(def test-config (edn/read-string (slurp "dev/resources/test-config.edn")))
```

```clojure
(def test-yurt (yurt/build (yurt/blueprint) 
                           {:swap {"neo.conf/config" test-config}}))
```

`:swap` takes a map of component names and their substitutes. In this case `test-config` is a substitute for the value of `"neo.conf/config"` component.

## Building Smaller Yurts

A Yurt can be built with `only` certain components specified with an `:only` option:

```clojure
dev=> (def bp (yurt/blueprint))

dev=> (def small-yurt (yurt/build (yurt/blueprint)
                                  {:only #{"neo.conf/config"
                                           "neo.app/nrepl"}}))

INFO  utils.logging - >> starting.. #'neo.conf/config
INFO  neo.conf - loading config from dev/resources/config.edn
INFO  utils.logging - >> starting.. #'neo.app/nrepl
#'dev/small-yurt
dev=>
```

`:only` option that takes a sequence of component names to start, or in other words, components that this Yurt should be built from. In this case `#{"neo.conf/config" "neo.app/nrepl"}`.

Here is what the built Yurt looks like:

```clojure
dev=> (pprint small-yurt)
{:components
 {"neo.conf/config"
  {:datomic {:uri "datomic:mem://yurt"},
   :www {:port 4242},
   :nrepl {:host "0.0.0.0", :port 7878}},
  "neo.app/nrepl"
  #clojure.tools.nrepl.server.Server{:server-socket #object[java.net.ServerSocket 0x7e5ffa33 "ServerSocket[addr=/0.0.0.0,localport=7878]"], :port 7878, :open-transports #object[clojure.lang.Atom 0x6d7364 {:status :ready, :val #{}}], :transport #object[clojure.tools.nrepl.transport$bencode 0x7a4c511f "clojure.tools.nrepl.transport$bencode@7a4c511f"], :greeting nil, :handler #object[clojure.tools.nrepl.middleware$wrap_conj_descriptor$fn__2241 0x3ca87a46 "clojure.tools.nrepl.middleware$wrap_conj_descriptor$fn__2241@3ca87a46"], :ss #object[java.net.ServerSocket 0x7e5ffa33 "ServerSocket[addr=/0.0.0.0,localport=7878]"]}},
 :blueprint
 {"neo.conf/config" {:order 1},
  "neo.db/db" {:order 2},
  "neo.www/neo-app" {:order 3},
  "neo.app/nrepl" {:order 4}}}
```

i.e. only `"neo.conf/config"` and `"neo.app/nrepl"` are the components of this Yurt.

and when this yurt is destroyed:
```clojure
dev=> (yurt/destroy small-yurt)
{:stopped #{"neo.app/nrepl"}}
```

only the components that were part of the Yurt were stopped. In this case `"neo.app/nrepl"`,
since `"neo.conf/config"` does not have a "`:stop`" function.

## Declarative Yurts

Yurts can be configured declaritively with both a `:swap` map and an `:only` sequence:

```clojure
dev=> (def test-yurt (yurt/build (yurt/blueprint)
                                 {:swap {"neo.conf/config" test-config}
                                  :only ["neo.conf/config" "neo.db/db"]}))
```

which would not start `neo.www/neo-app` _and_ would swap config with a test one:

```clojure
dev=> ((-> test-yurt :components) "neo.www/neo-app")
nil
```

```clojure
dev=> ((-> test-yurt :components) "neo.conf/config")
{:datomic {:uri "datomic:mem://test-yurt"},
 :www {:port 4200},
 :nrepl {:host "0.0.0.0", :port 7800}}
```

## Stop functions

The only thing Yurt requires from `defstate`s is that their `:stop` functions are 1 arity (i.e. take one argument). When the `(yurt/destroy)` is called, it would pass the actual instance of a component to this `:stop` function.

For example:

```clojure
(defstate nrepl :start (start-nrepl (:nrepl config))
                :stop stop-server)
```

where `stop-server` is `clojure.tools.nrepl.server/stop-server` function that takes a server to stop.

In case a stop function needs to take _more_ than one argument it could just take a map. Here is [an example](https://github.com/tolitius/yurt/blob/632ce37f7fb11fbc1c0a0dfc76e47305c954d77d/dev/clj/neo/db.clj#L11-L24).

One arity `:stop` functions is _the only_ requirement Yurt has for the mount app.

## Show me

sure.

```shell
$ boot repl
```

```clojure
boot.user=> (dev)
#object[clojure.lang.Namespace 0x61647fa2 "dev"]
```

Working with a [neo](dev/clj/neo) `mount` sample app that comes with Yurt sources and has 4 components (`mount` states):

* `config`, loaded from the files and refreshed on each (reset)
* `datomic connection` that uses the config to create itself
* `nyse web app` which is a web server with compojure routes (i.e. the actual app)
* `nrepl` that uses config to bind to host/port

First before building Yurts, let's checkout the blueprint:

```clojure
dev=> (yurt/blueprint)
{:components
 {"neo.conf/config" {:status :not-started},
  "neo.db/db" {:status :not-started},
  "neo.www/neo-app" {:status :not-started},
  "neo.app/nrepl" {:status :not-started}},
 :blueprint
 {"neo.conf/config" {:order 1},
  "neo.db/db" {:order 2},
  "neo.www/neo-app" {:order 3},
  "neo.app/nrepl" {:order 4}}}
```

Now let's build a dev Yurt that's based on [this](dev/resources/config.edn) config:

```clojure
dev=> (def dev-yurt (yurt/build (yurt/blueprint)))
INFO  neo.conf - loading config from dev/resources/config.edn
INFO  neo.db - conf:  {:datomic {:uri datomic:mem://yurt}, :www {:port 4242}, :nrepl {:host 0.0.0.0, :port 7878}}
INFO  neo.db - creating a connection to datomic: datomic:mem://yurt
#'dev/dev-yurt
```

notice the config ports an datomic uri ^^^.

Let's look at what we've built:

```clojure
dev=> dev-yurt
{:components
 {"neo.conf/config"
  {:datomic {:uri "datomic:mem://yurt"},
   :www {:port 4242},
   :nrepl {:host "0.0.0.0", :port 7878}},
  "neo.db/db"
  {:conn
   #object[datomic.peer.LocalConnection 0x3f66af9c "datomic.peer.LocalConnection@3f66af9c"],
   :uri "datomic:mem://yurt"},
  "neo.www/neo-app"
  #object[org.eclipse.jetty.server.Server 0x2dd20d61 "org.eclipse.jetty.server.Server@2dd20d61"],
  "neo.app/nrepl"
  #clojure.tools.nrepl.server.Server{:server-socket #object[java.net.ServerSocket 0x3ebc5516 "ServerSocket[addr=/0.0.0.0,localport=7878]"], :port 7878, :open-transports #object[clojure.lang.Atom 0x7026e6db {:status :ready, :val #{}}], :transport #object[clojure.tools.nrepl.transport$bencode 0x38a2d586 "clojure.tools.nrepl.transport$bencode@38a2d586"], :greeting nil, :handler #object[clojure.tools.nrepl.middleware$wrap_conj_descriptor$fn__1707 0x3c114a1 "clojure.tools.nrepl.middleware$wrap_conj_descriptor$fn__1707@3c114a1"], :ss #object[java.net.ServerSocket 0x3ebc5516 "ServerSocket[addr=/0.0.0.0,localport=7878]"]}},
 :blueprint
 {"neo.conf/config" {:order 1},
  "neo.db/db" {:order 2},
  "neo.www/neo-app" {:order 3},
  "neo.app/nrepl" {:order 4}}}
```

Now let's build a test (second) Yurt based on [this](dev/resources/test-config.edn) test config:

```clojure
;; reading the test config in..
dev=> (require '[clojure.edn :as edn])
dev=> (def test-config (edn/read-string (slurp "dev/resources/test-config.edn")))
#'dev/test-config
```

notice we are building it by the same blueprint:

```clojure
dev=> (def test-yurt (yurt/build (yurt/blueprint) {:swap {"neo.conf/config" test-config}}))
INFO  neo.db - conf:  {:datomic {:uri datomic:mem://test-yurt}, :www {:port 4200}, :nrepl {:host 0.0.0.0, :port 7800}}
INFO  neo.db - creating a connection to datomic: datomic:mem://test-yurt
#'dev/test-yurt
```

just substituting the config component _with_ the test config:

```clojure
;; e.g. (yurt/build (yurt/blueprint) {:swap {"neo.conf/config" test-config}})
```

notice the config ports an datomic uri ^^^.

we can substitute as many components as we want since `:swap` takes a map where keys are the state names, and values are the substitutes (i.e. any values).

Let's look at what we've built:

```clojure
dev=> test-yurt
{:components
 {"neo.conf/config"
  {:datomic {:uri "datomic:mem://test-yurt"},
   :www {:port 4200},
   :nrepl {:host "0.0.0.0", :port 7800}},
  "neo.db/db"
  {:conn
   #object[datomic.peer.LocalConnection 0x48b2fa4 "datomic.peer.LocalConnection@48b2fa4"],
   :uri "datomic:mem://test-yurt"},
  "neo.www/neo-app"
  #object[org.eclipse.jetty.server.Server 0x77fb2bac "org.eclipse.jetty.server.Server@77fb2bac"],
  "neo.app/nrepl"
  #clojure.tools.nrepl.server.Server{:server-socket #object[java.net.ServerSocket 0xbc92366 "ServerSocket[addr=/0.0.0.0,localport=7800]"], :port 7800, :open-transports #object[clojure.lang.Atom 0x1bde6216 {:status :ready, :val #{}}], :transport #object[clojure.tools.nrepl.transport$bencode 0x38a2d586 "clojure.tools.nrepl.transport$bencode@38a2d586"], :greeting nil, :handler #object[clojure.tools.nrepl.middleware$wrap_conj_descriptor$fn__1707 0x2edf365d "clojure.tools.nrepl.middleware$wrap_conj_descriptor$fn__1707@2edf365d"], :ss #object[java.net.ServerSocket 0xbc92366 "ServerSocket[addr=/0.0.0.0,localport=7800]"]}},
 :blueprint
 {"neo.conf/config" {:order 1},
  "neo.db/db" {:order 2},
  "neo.www/neo-app" {:order 3},
  "neo.app/nrepl" {:order 4}}}
dev=>
```

Let's look deep inside the Yurts and see, for example, if their Jetty web servers are running:

```clojure
dev=> (.isStarted ((-> dev-yurt :components) "neo.www/neo-app"))
true
dev=> (.isStarted ((-> test-yurt :components) "neo.www/neo-app"))
true
```

Now let's destroy the test Yurt:

```clojure
dev=> (yurt/destroy test-yurt)
INFO  neo.db - disconnecting from  datomic:mem://test-yurt
```

Check the server statuses again:

```clojure
dev=> (.isStarted ((-> test-yurt :components) "neo.www/neo-app"))
false
dev=> (.isStarted ((-> dev-yurt :components) "neo.www/neo-app"))
true
```

notice that the test server is no longer running, but the development one is.

Let's destroy the development Yurt as well:

```clojure
dev=> (yurt/destroy dev-yurt)
INFO  neo.db - disconnecting from  datomic:mem://yurt
```

Check the server statuses again:

```clojure
dev=> (.isStarted ((-> test-yurt :components) "neo.www/neo-app"))
false
dev=> (.isStarted ((-> dev-yurt :components) "neo.www/neo-app"))
false
```

Great, we are now ready to build as many _local_, `mount` based Yurts as we'd like and run them _simultaneously_ in the same Clojure runtime.

## License

Copyright © 2018 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
