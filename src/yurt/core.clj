(ns yurt.core
  (:require [mount.core :as mount]))

(defprotocol Lifecycle
  (build [this] "builds a yurt")
  (destroy [this] "destroys a yurt")
  ;; (build-with [this components])
  ;; (build-without [this components])
  ;; (destroy-except [this components])
  )

(defrecord Yurt [components])

(defn- select-fun [states f]
  (into []
        (remove nil?
                (for [[name state] states]
                  (when-let [fun (f state)]
                    [name fun])))))

(defn- bring-down [components funs]
  (into {}
        (doseq [[name fun] funs]
          ((fun) (components name))
          [name :stopped])))

(defn- unvar-state [s]
  (->> s (drop 2) (apply str)))  ;; magic 2 is removing "#'" in state name

(defn unvar-names [states]
  (into {} (for [[k v] states]
             [(unvar-state k) v])))

(defn- not-started [states]
  (into {}
    (for [[state {:keys [order]}] states]
      [state {:status :not-started 
              :order order}])))

(defn- detach [sys]
  (doseq [[state {:keys [var status] :as v}] sys]
    (alter-var-root var (constantly :not-started))
    (#'mount.core/update-meta! [state :status] #{:stopped})
    (#'mount.core/update-meta! [state :var] :not-started)))

(defn- attach [sys]
  (into {}
        (for [[k {:keys [var]}] sys]
          [(unvar-state k) @var])))

;; TODO: this will take with / without / individual states
(defn- spawn [sys]
  (mount/start)
  (let [spawned (attach sys)]
    (detach sys)
    spawned))

(defn blueprint []
  (let [meta-state @@#'mount.core/meta-state
        states (-> (sort-by (comp :order val) < 
                            meta-state)
                    unvar-names)
        down (reverse (select-fun states :stop))]
    (extend-type Yurt
      Lifecycle
      (build [_] (->Yurt (spawn meta-state)))
      (destroy [it] (bring-yurt (:components it) down)))
    (->Yurt (not-started states))))

(comment

;; REPLing it

(require '[yurt.core :as yurt])
(def u (yurt/blueprint))
(def u (yurt/build u))

;; at this point the yurt is up and detached: i.e. "global" vars and mount do not see it
)
