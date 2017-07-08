(ns dev
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :as tn]
            [boot.core :refer [load-data-readers!]]
            [mount.core :as mount]
            [yurt.core :as yurt]
            [mount.tools.graph :refer [states-with-deps]]
            [utils.logging :refer [with-logging-status]]
            [neo.stager :refer [orders stage-order-book]]
            [neo.app]
            [neo.db :refer [db]]
            [neo.options.engine :refer [match-quote]]
            [neo.options.orders :refer [find-orders add-order]]))

(with-logging-status)

(defn refresh []
  (tn/refresh))

(defn refresh-all []
  (tn/refresh-all))

(defn reset []
  (refresh))
