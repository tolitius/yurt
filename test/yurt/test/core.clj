(ns yurt.test.core
  (:require [yurt.core :as yurt]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]))

(deftest multiple-yurts

  (testing "two yurts can be started in the same runtime"
    (let [blue (yurt/blueprint)
          a-yurt (yurt/build blue)
          b-config (edn/read-string (slurp "dev/resources/test-config.edn"))
          b-yurt (yurt/build-with blue {"neo.conf/config" b-config})
          a-www ((-> a-yurt :components) "neo.www/neo-app")
          b-www ((-> b-yurt :components) "neo.www/neo-app")]

      (is (.isStarted a-www))
      (is (.isStarted b-www))

      (yurt/destroy b-yurt)
      (is (.isStarted a-www))
      (is (not (.isStarted b-www)))

      (yurt/destroy a-yurt)
      (is (not (.isStarted a-www)))
      (is (not (.isStarted b-www))))))
