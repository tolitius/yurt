(ns yurt.core
  (:require [mount.core :as mount]))

(defprotocol Hut
  (build [this] "builds a hut")
  (destroy [this] "destroys a hut")
  (build-with [this substitutes] "builds a hut with some components substituted")
  (build-only [this components] "builds a hut out of only components provided")
  (build-only-with [this components substitutes] "builds a hut of only components provided, with substitutions"))
  ;; (build-without [this components])
  ;; (destroy-except [this components])
  

(defrecord Yurt [components blueprint])

(defn- select-fun [states f]
  (into []
        (remove nil?
                (for [[name state] states]
                  (when-let [fun (f state)]
                    [name fun])))))

(defn- bulldoze [components funs]
  (reduce (fn [cs [name fun]]
            (let [component (components name)]
              (if (and component                        ;; only destroy existing components
                       (not= component :not-started))   ;; that were started
                (do ((fun) component)
                    (conj cs name))
                cs)))
          #{}
          funs))

(defn- unvar-state [s]
  (->> s (drop 2) (apply str)))  ;; magic 2 is removing "#'" in state name

(defn- var-state [s]
  (str "#'" s))

(defn unvar-names [states]
  (into {} (for [[k v] states]
             [(unvar-state k) v])))

(defn- detach-state [state status]
  (#'mount.core/alter-state! status :not-started)
  (swap! @#'mount.core/running dissoc state)
  (#'mount.core/update-meta! [state :status] #{:stopped}))

(defn- detach [sys]
  (doseq [[state status] sys]
    (detach-state state status)
    (#'mount.core/rollback! state)))

(defn- var-comp [xs]
  (into #{} (map var-state xs)))

(defn- attach [sys & {:keys [only]}]
  (let [in-scope? (var-comp only)
        no-scope? (empty? in-scope?)]
    (into {}
          (for [[k {:keys [var]}] sys]
            (when (or no-scope?
                      (in-scope? k))
                [(unvar-state k) @var])))))

(defn- var-subs [m]
  (into {}
        (for [[k v] m]
          [(var-state k) v])))

;; TODO: combinations of only/swap/etc..
(defn- spawn [sys & {:keys [swap only] :as ops}]
  (condp = (keys ops)
    [:only :swap] (-> (mount/only (var-comp only))
                      (mount/swap (var-subs swap))
                      (mount/start))
    [:swap] (mount/start-with (var-subs swap))
    [:only] (mount/start (var-comp only))
    (mount/start))
  (let [spawned (attach @sys :only only)]
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
      (build-only [_ components] (->Yurt (spawn meta-state :only components) bp))
      (build-only-with [_ components substitutes] (->Yurt (spawn meta-state :only components :swap substitutes) bp))
      (destroy [it] {:stopped (bulldoze (:components it) stop-fns)}))
    (->Yurt (not-started states) bp)))
