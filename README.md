# yurt

  module  |  branch  |  status
----------|----------|----------
   yurt   | `master` | [![Circle CI](https://circleci.com/gh/tolitius/yurt/tree/master.png?style=svg)](https://circleci.com/gh/tolitius/yurt/tree/master)

[![Clojars Project](http://clojars.org/yurt/latest-version.svg)](http://clojars.org/yurt)

> <img src="doc/img/slack-icon.png" width="30px"> _any_ questions or feedback: [`#mount`](https://clojurians.slack.com/messages/mount/) clojurians slack channel (or just [open an issue](https://github.com/tolitius/mount/issues))

## What is it for?

Building standalone application Yurts with [mount](https://github.com/tolitius/mount).

Multiple brand new _local_ Yurts with components can be created and passed down to the application / REPL to be used simultaneously for fun and profit.

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
dev=> (def test-yurt (yurt/build-with (yurt/blueprint) {"neo.conf/config" test-config}))
INFO  neo.db - conf:  {:datomic {:uri datomic:mem://test-yurt}, :www {:port 4200}, :nrepl {:host 0.0.0.0, :port 7800}}
INFO  neo.db - creating a connection to datomic: datomic:mem://test-yurt
#'dev/test-yurt
```

just substituting the config component _with_ the test config:

```clojure
;; e.g. (yurt/build-with (yurt/blueprint) {"neo.conf/config" test-config})
```

notice the config ports an datomic uri ^^^.

we can substitute as many components as we want since `build-with` takes a map where keys are the state names, and values are the substitutes (i.e. any values).

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

Great, we are now ready to build as many _local_, `mount` based Yurts as we'd like and run them _simultaniously_ in the same JVM.

## License

Copyright © 2015 tolitius

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
