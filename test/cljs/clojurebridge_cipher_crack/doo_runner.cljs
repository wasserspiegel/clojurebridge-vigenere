(ns clojurebridge-cipher-crack.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [clojurebridge-cipher-crack.core-test]))

(doo-tests 'clojurebridge-cipher-crack.core-test)
