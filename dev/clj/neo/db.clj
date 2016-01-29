(ns neo.db
  (:require [mount.core :refer [defstate]]
            [datomic.api :as d]
            [clojure.tools.logging :refer [info]]
            [neo.conf :refer [config]]))

(def boo (do
           (#'clojure.core/load-data-readers)
           (set! *data-readers* (.getRawRoot #'*data-readers*))))

(defn- new-connection [conf]
  (info "conf: " conf)
  (let [uri (get-in conf [:datomic :uri])]
    (info "creating a connection to datomic:" uri)
    (d/create-database uri)
    {:conn (d/connect uri) :uri uri}))

(defn disconnect [{:keys [conn uri]}]
  (info "disconnecting from " uri)
  (.release conn)                     ;; usually it's not released, here just to illustrate the access to connection on (stop)
  (d/delete-database uri))

(defstate db :start (new-connection config)
             :stop disconnect)

;; datomic schema (staging for an example)
(defn create-schema [conn]
  (let [schema [{:db/id #db/id [:db.part/db]
                 :db/ident :order/symbol
                 :db/valueType :db.type/string
                 :db/cardinality :db.cardinality/one
                 :db/index true
                 :db.install/_attribute :db.part/db}

                {:db/id #db/id [:db.part/db]
                 :db/ident :order/bid
                 :db/valueType :db.type/bigdec
                 :db/cardinality :db.cardinality/one
                 :db.install/_attribute :db.part/db}
                
                {:db/id #db/id [:db.part/db]
                 :db/ident :order/qty
                 :db/valueType :db.type/long
                 :db/cardinality :db.cardinality/one
                 :db.install/_attribute :db.part/db}

                {:db/id #db/id [:db.part/db]
                 :db/ident :order/offer
                 :db/valueType :db.type/bigdec
                 :db/cardinality :db.cardinality/one
                 :db.install/_attribute :db.part/db}]]

        @(d/transact conn schema)))
