(require '[boot.core :refer :all]                           ; IntelliJ "integration"
         '[boot.task.built-in :refer :all])

(task-options!
  pom {:project     'clojurebridge-cipher-crack
       :version     "0.1"
       :description "clojurebridge-cipher"
       :license     {"EPL" "@wasserspiegel"}})

(set-env!
  :dependencies '[[org.clojure/clojure       "1.9.0-alpha14"            :scope "provided"]
                  [org.clojure/clojurescript "1.9.521"          :scope "compile"]
                  [org.clojure/core.async    "0.2.395"]

                  ;;; database, server & logging ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

                  [pandeiro/boot-http        "0.7.6"   :scope "test"]

                  ;;; cljc  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                  ;; [com.cognitect/transit-clj      "0.8.259"]
                  ;; [cljs-ajax                        "0.5.9"]
                  ;;; backend framework  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


                  ;;; frontend framework  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                  [reagent                          "0.6.2"]
                  [reagent-utils                    "0.2.1"]
                   [secretary "1.2.3"]
                  [venantius/accountant "0.2.0"
                  :exclusions [org.clojure/tools.reader]]
                  ;;; boot  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                  [boot/core                 "2.7.1"            :scope "provided"]
                  [onetom/boot-lein-generate "0.1.3"            :scope "test"]
                  [adzerk/boot-cljs          "1.7.228-2"        :scope "compile"]
                  [adzerk/boot-reload        "0.4.13"           :scope "compile"]

                  ;;; cljs-repl  ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                  [adzerk/boot-cljs-repl     "0.3.3"]

                  ;;; cljs repl ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
                  [binaryage/dirac           "1.2.8"            :scope "test"]
                  [binaryage/devtools        "0.9.4"            :scope "test"]
                  [powerlaces/boot-cljs-devtools "0.2.0"        :scope "test"]



                  ]
  :source-paths   #{"src/cljc" "src/cljs"}
  :resource-paths #{"resources"}
  :asset-paths    #{"assets"}
  )

;;create project file for cursive
(require
 '[boot.lein])
(boot.lein/generate)


(require
  '[adzerk.boot-reload              :refer [reload]]
  '[adzerk.boot-cljs                :refer [cljs]]
  '[powerlaces.boot-cljs-devtools   :refer [cljs-devtools dirac]]
  '[adzerk.boot-cljs-repl           :refer [cljs-repl start-repl]]
  '[pandeiro.boot-http              :refer [serve]]

  )

;
(def devtools-config
 {:features-to-install           [:formatters :hints :async]
  :dont-detect-custom-formatters true})

(def dirac-config
 {:nrepl-config {;reveal-url-script-path "scripts/reveal.sh"
                 ;reveal-url-request-handler (fn [config url line column]
                 ;                              (str "ERR REPLY>" url))
                 }}  )

(deftask dev []
         (comp
           (watch)
           (speak :theme "woodblock")
           (reload :on-jsload 'clojurebridge-cipher-crack.core/main)
           (cljs-devtools)
           ;(cljs-repl)
           (cljs :compiler-options {:optimizations   :none
                                    :parallel-build  true
                                    :source-map      true
                                    })
           (target)
           (serve :port 3000)))

(deftask dirac-dev []
         (comp (watch) (speak :theme "woodblock")
               (cljs-devtools) (dirac)
               (cljs :compiler-options {:optimizations   :none
                                        :parallel-build  true
                                        :source-map      true
                                        :preloads        ["devtools.preload" "dirac.runtime.preload"]
                                        :main            "chef.core"
                                        :external-config {:devtools/config devtools-config
                                                          :dirac.runtime/config dirac-config}
                     } )
               (target)
               (serve :port 3003)
               ))

(deftask prod
         []
         (comp 
          (cljs :optimizations :advanced)
          (target :dir #{"static"})))
