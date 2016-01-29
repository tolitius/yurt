(ns neo.stager
  (:require [neo.options.orders :refer [add-order]]))

(def orders [{:ticker "GOOG" :bid 665.51M :offer 665.59M :qty 100}
             {:ticker "GOOG" :bid 665.50M :offer 665.58M :qty 300}
             {:ticker "GOOG" :bid 665.48M :offer 665.53M :qty 100}
             {:ticker "GOOG" :bid 665.49M :offer 665.52M :qty 200}

             {:ticker "TSLA" :bid 232.38M :offer 232.43M :qty 200}
             {:ticker "TSLA" :bid 232.41M :offer 232.46M :qty 100}])

(defn stage-order-book [conn orders]
  (map (partial add-order conn) orders))
