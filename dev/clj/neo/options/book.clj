(ns neo.options.book
  (:require [datomic.api :as d]
            [utils.datomic :refer [touch]]))

(defn top-of-the-book [conn])
(defn price-levels [conn])
(defn book-depth [conn])
;; ... 
