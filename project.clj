(defproject
  clojurebridge-cipher-crack
  "0.1"
  :repositories
  [["clojars" {:url "https://repo.clojars.org/"}]
   ["maven-central" {:url "https://repo1.maven.org/maven2"}]]
  :dependencies
  [[org.clojure/clojure "1.9.0-alpha14" :scope "provided"]
   [org.clojure/clojurescript "1.9.521" :scope "compile"]
   [org.clojure/core.async "0.2.395"]
   [pandeiro/boot-http "0.7.6" :scope "test"]
   [reagent "0.6.2"]
   [reagent-utils "0.2.1"]
   [secretary "1.2.3"]
   [venantius/accountant
    "0.2.0"
    :exclusions
    [org.clojure/tools.reader]]
   [boot/core "2.7.1" :scope "provided"]
   [onetom/boot-lein-generate "0.1.3" :scope "test"]
   [adzerk/boot-cljs "1.7.228-2" :scope "compile"]
   [adzerk/boot-reload "0.4.13" :scope "compile"]
   [adzerk/boot-cljs-repl "0.3.3"]
   [binaryage/dirac "1.2.8" :scope "test"]
   [binaryage/devtools "0.9.4" :scope "test"]
   [powerlaces/boot-cljs-devtools "0.2.0" :scope "test"]]
  :source-paths
  ["src/cljs" "src/cljc"]
  :resource-paths
  ["resources"])