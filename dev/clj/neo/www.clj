(ns neo.www
  (:require [neo.options.orders :refer [add-order find-orders]]
            [neo.db :refer [db create-schema]]
            [neo.conf :refer [config]]
            [neo.options.engine :refer [match-quote]]
            [neo.stager :refer [stage-order-book orders]]
            [mount.core :refer [defstate]]
            [cheshire.core :refer [generate-string]]
            [compojure.core :refer [routes defroutes GET POST]]
            [compojure.handler :as handler]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn- to-order [ticker qty bid offer]
  ;; yes, validation :)
  {:ticker ticker 
   :bid (bigdec bid) 
   :offer (bigdec offer) 
   :qty (Integer/parseInt qty)})

(defn make-routes [db]
  (defroutes neo-routes

    (GET "/" [] "welcome to neo options exchange!")

    (GET "/neo/orders/:ticker" [ticker]
         (generate-string (find-orders db ticker)))

    (GET "/neo/match-quote" [ticker qty bid offer]
         (let [book (find-orders db ticker)
               quote (to-order ticker qty bid offer)]
           (generate-string {:matched (match-quote quote book)})))

    (POST "/neo/orders" [ticker qty bid offer] 
          (let [order (to-order ticker qty bid offer)]
            (add-order db order)
            (generate-string {:added order})))))

(defn start-neo [{:keys [conn]} {:keys [www]}]  ;; app entry point
  (create-schema conn)                          ;; just an example, usually schema would already be there
  (stage-order-book conn orders)                ;; just an example, usually data will already be there
  (-> (routes (make-routes conn))
      (handler/site)
      (run-jetty {:join? false
                  :port (:port www)})))

(defstate neo-app :start (start-neo db config)
                  :stop #(.stop %))  ;; it's a "org.eclipse.jetty.server.Server" at this point
