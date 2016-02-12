(ns yurt.core
  (:require [mount.core :as mount]))

(defprotocol Hut
  (build [this] "builds a hut")
  (destroy [this] "destroys a hut")
  (build-with [this substitutes] "builds a hut with some components substituted")
  ;; (build-without [this components])
  ;; (destroy-except [this components])
  )

(defrecord Yurt [components blueprint])

(defn- select-fun [states f]
  (into []
        (remove nil?
                (for [[name state] states]
                  (when-let [fun (f state)]
                    [name fun])))))

(defn- bulldoze [components funs]
  (into {}
        (doseq [[name fun] funs]
          ((fun) (components name))
          [name :stopped])))

(defn- unvar-state [s]
  (->> s (drop 2) (apply str)))  ;; magic 2 is removing "#'" in state name

(defn- var-state [s]
  (str "#'" s))

(defn unvar-names [states]
  (into {} (for [[k v] states]
             [(unvar-state k) v])))

(defn- detach [sys]
  (doseq [[state status] sys]
    (#'mount.core/down state status (atom []))))

(defn- attach [sys]
  (into {}
        (for [[k {:keys [var]}] sys]
          [(unvar-state k) @var])))

(defn- var-subs [m]
  (into {}
        (for [[k v] m]
          [(var-state k) v])))

;; TODO: this will take with / without / individual states
(defn- spawn [sys & {:keys [swap]}]
  (if-not swap
    (mount/start)
    (mount/start-with (var-subs swap)))
  (let [spawned (attach @sys)]
    (detach @sys)
    spawned))

(defn- not-started [states]
  (into {}
    (for [[state {:keys [order]}] states]
      [state {:status :not-started}])))

(defn- new-blueprint [states]
  (into {}
    (for [[state {:keys [order]}] states]
      [state {:order order}])))

(defn blueprint []
  (let [meta-state @#'mount.core/meta-state
        states (-> (sort-by (comp :order val) < 
                            @meta-state)
                    unvar-names)
        bp (new-blueprint states)
        stop-fns (reverse (select-fun states :stop))]
    (extend-type Yurt
      Hut
      (build [_] (->Yurt (spawn meta-state) bp))
      (build-with [_ substitutes] (->Yurt (spawn meta-state :swap substitutes) bp))
      (destroy [it] (bulldoze (:components it) stop-fns)))
    (->Yurt (not-started states) bp)))
