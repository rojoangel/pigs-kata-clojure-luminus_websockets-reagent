(ns multi-client-ws.core-test
  (:require [cljs.test :refer-macros [is are deftest testing use-fixtures]]
            [pjstadig.humane-test-output]
            [multi-client-ws.core :as rc]))

(deftest test-home
  (is (= true true)))

