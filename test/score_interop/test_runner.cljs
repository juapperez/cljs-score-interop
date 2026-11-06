(ns score-interop.test-runner
  (:require [cljs.test :as test] [score-interop.core-test]))
(defmethod test/report [::test/default :end-run-tests] [result]
  (when-not (test/successful? result) (set! (.-exitCode js/process) 1)))
(defn -main [] (test/run-tests 'score-interop.core-test))
