(ns clojurebridge-cipher-crack.prod
  (:require [clojurebridge-cipher-crack.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/main)
