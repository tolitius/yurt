(ns neo.options.engine)

(defn match-to
  "matches quotes to orders. 
  (no crossed book logic, just a simple bid to offer comparisson)
  in case there is a match returns the minimum of to quantities
  returns 0 on no match"
  [{q-bid :bid q-offer :offer q-qty :qty}                   ;; "q" for qoute
   {:keys [:order/bid :order/offer :order/qty] :as order}]
    (when qty
      (if (or (>= q-bid offer) 
              (<= q-offer bid))
        (min q-qty qty)
        0)))

(defn match-available
  "returns total matched quantity available on the book"
  [quote book]
  (reduce + (map (partial match-to quote) 
                 book)))

(defn match-quote 
  "looks for total book availability and, if matched
   limits it to the quote quantity"
  [{:keys [qty] :as quote} book]
  (let [matched (match-available quote book)]
    (if (pos? matched)
      (min qty matched)
      matched)))
