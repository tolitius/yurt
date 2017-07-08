(def +version+ "0.1.2-SNAPSHOT")

(set-env!
  :source-paths #{"src"}
  :dependencies
  '[[mount                          "0.1.11"]
    [org.clojure/clojure            "1.8.0"]

    ;; deps for sample apps
    [compojure                      "1.6.0"           :scope "provided"]
    [ring/ring-jetty-adapter        "1.1.1"           :scope "provided"]
    [cheshire                       "5.7.1"           :scope "provided"]
    [ch.qos.logback/logback-classic "1.2.3"           :scope "provided"]
    [org.clojure/tools.logging      "0.4.0"           :scope "provided"]
    [robert/hooke                   "1.3.0"           :scope "provided"]
    [org.clojure/tools.namespace    "0.2.11"          :scope "provided"]
    [org.clojure/tools.nrepl        "0.2.13"          :scope "provided"]
    [com.datomic/datomic-free       "0.9.5561.50"     :scope "provided"]

    ;; boot clj
    [boot/core                      "2.7.1"           :scope "provided"]
    [adzerk/bootlaces               "0.1.13"          :scope "test"]
    [adzerk/boot-logservice         "1.1.0"           :scope "test"]
    [adzerk/boot-test               "1.2.0"           :scope "test"]
    [tolitius/boot-stripper         "0.1.0-SNAPSHOT"  :scope "test"]
    [tolitius/boot-check            "0.1.4"           :scope "test"]])

; The repl task updates the data-readers, BUT only after it loads the
; file specified with the :init option. That file however already contains
; #db/id reader tags, hence trying to load it would fail.
;
; We cannot run this from the dev task either, because it throws this exception:
;   java.lang.IllegalStateException: Can't set!: *data-readers* from non-binding thread
(load-data-readers!)

(require '[adzerk.bootlaces :refer :all]
         '[tolitius.boot-check :as check]
         '[tolitius.boot-stripper :refer [strip-deps-attr]]
         '[clojure.tools.namespace.repl :refer [set-refresh-dirs]]
         '[adzerk.boot-test :as bt]
         '[adzerk.boot-logservice :as log-service]
         '[clojure.tools.logging :as log])

(bootlaces! +version+)

(def log4b
  [:configuration
   [:appender {:name "STDOUT" :class "ch.qos.logback.core.ConsoleAppender"}
    [:encoder [:pattern "%-5level %logger{36} - %msg%n"]]]
   [:root {:level "TRACE"}
    [:appender-ref {:ref "STDOUT"}]]])

(deftask dev []
  (set-env! :source-paths #(conj % "dev/clj"))

  (task-options!
    repl {:init "dev.clj"
          :init-ns 'dev})

  (alter-var-root #'log/*logger-factory*
                  (constantly (log-service/make-factory log4b)))

  (apply set-refresh-dirs (get-env :directories)))

(deftask test []
  (set-env! :source-paths #(conj % "test" "dev/clj"))
  (bt/test))

(deftask check-sources []
  (comp
    (check/with-bikeshed)
    (check/with-eastwood)
    (check/with-yagni)
    (check/with-kibit)))

(task-options!
  push {:ensure-branch nil}
  pom {:project     'yurt
       :version     +version+
       :description "high quality mounted real (e)states"
       :url         "https://github.com/tolitius/yurt"
       :scm         {:url "https://github.com/tolitius/yurt"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})
