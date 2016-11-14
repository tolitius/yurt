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
      (is (not (.isStarted b-www)))))

  (testing "yurt can be build with a specified scope of components"
    (let [blue (yurt/blueprint)
          yurt (yurt/build-only blue #{"neo.conf/config" "neo.app/nrepl"})
          www ((-> yurt :components) "neo.www/neo-app")
          conf ((-> yurt :components) "neo.conf/config")
          db ((-> yurt :components) "neo.db/db")
          repl ((-> yurt :components) "neo.app/nrepl")]

      (is (not www))
      (is (not db))
      (is conf)
      (is (not (.isClosed (:server-socket repl))))
      
      (is (= (yurt/destroy yurt) {:stopped #{"neo.app/nrepl"}}))
      (is (.isClosed (:server-socket repl))))))
