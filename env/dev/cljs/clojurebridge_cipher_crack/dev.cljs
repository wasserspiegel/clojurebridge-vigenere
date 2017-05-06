(ns ^:figwheel-no-load clojurebridge-cipher-crack.dev
  (:require [clojurebridge-cipher-crack.core :as core]
            [figwheel.client :as figwheel :include-macros true]))

(enable-console-print!)

(core/init!)
