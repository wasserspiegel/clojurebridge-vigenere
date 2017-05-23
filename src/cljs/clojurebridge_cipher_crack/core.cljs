(ns clojurebridge-cipher-crack.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [clojurebridge_cipher_crack.vigenere :as v]
              [dirac.runtime :as dirac-rt]
              [goog.string :as gstring]
              [goog.string.format]
              [goog.events :as events])
              (:import goog.History))

(defn format
  [fmt & args]
  (apply gstring/format fmt args))

;; state
(defonce ui-state (reagent/atom {:message v/default-message :cipher "clojurebridge" :encrypt true :cr-cl "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"}))

;; event functions
(defn nrange [s] (mapv js/Number. (clojure.string/split s #"\s")))
(defn encrypt-message [] (swap! ui-state assoc-in [:encr]
                                       (v/vinegre-crypt 
                                         (v/e-map (get @ui-state :cipher)) 
                                         (v/strip-non-alpha (get @ui-state :message)))))
(defn decrypt-message [] (swap! ui-state assoc-in [:message]
                                       (v/vinegre-crypt 
                                         (v/d-map (get @ui-state :cipher))
                                         (v/strip-non-alpha (get @ui-state :encr)))))
(defn ui-state-updater [path]  (fn [ev] (swap! ui-state assoc-in path (.-value (.-target ev)))))
(defn ui-state-toggle  [path]  (swap! ui-state update-in path not))





(def scale1
  "basic lin scale green, blue, red"
  (let [mp 8 ;midpoint
        r    (fn [x] (- (* mp mp (- x mp) 256)))
        g    (fn [x] (- (* mp mp (- mp x) 256)))
        b    (fn [x] (- 255 (* mp (- mp x) (- mp x))))
        a    (fn [_] 255)]
  [r g b a]
))

(def  ^:export scale2
  "input 0.01-200 log ~ -2 - +5
   mp midpoint
   sc scale
   mph midpoint height keeps av.col. above half across the full range"
  (let [mp 1.8 ;midpoint
        sc 32
        mph 128
        r    (fn [x] (+ mph (* 2 sc (- (.log js/Math x) mp))))
        g    (fn [x] (- mph (* 2 sc (- (.log js/Math x) mp))))
        ;b    (fn [_] 0) ; looks almost as good
        b    (fn [x] (- 256 sc (* sc (- (.log js/Math x) mp) (- (.log js/Math x) mp))))
        a    (fn [_] 255)]
  [r g b a]
))

(defn mat2img-data-fs 
  "turn var matrix into heatmap image data
     except for the typed array as we have to mutate the array provided by react 
     otherwise it will be replaced
     moved the heatmap scale functions to separate const."
  [mat]
  (let [is (range (count mat)) ; let's not assume the heatmap is square although it would be fair
        js (range (count (first mat)))
        fs   scale2   ]
  ;; [red green blue alpha]
  (vec (for [i is j js f fs] (f (get-in mat [i j]))))
))

(defn ^:export draw 
  "gets called just after the empty heatmap images are created in dom to fill the data"
  [id mat]
  (let [cnv (.getElementById js/document id)
        ctx (.getContext cnv "2d")
        img-data (.getImageData ctx 0 0 a-len a-len)
        data (.-data img-data)
        mdata (mat2img-data-fs mat)]
   ;; (.log js/console  (str "draw:  " id " head: arg.data:" (take 10 mdata)))
  (doall (for [i (range (* a-len a-len))] (aset data i (js/Number (get mdata i)))))
  (.putImageData ctx img-data 0 0)
   ;; (.log js/console  (str "draw:  " id " head image.data: " (aget data 0) (aget data 1) (aget data 2) (aget data 3)))   
  ))


  

(defn analyze [] (swap! ui-state assoc-in [:candidates]
                        (v/candidates (nrange (get @ui-state :cr-cl)) (get @ui-state :encr))))


;; reagent components
(defn ^:export imats 
  "create heatmap images"
  [id mat]
  (let [mid (str "mat-" id) ]
    (reagent/create-class
     {:component-did-mount #(draw mid mat)
      :reagent-render (fn [] [:canvas {:id mid :width "26" :height "26"} ] ) })
))

(defn ^:export result-component 
  "display a table of the results, the first result is sufficient for this demo"
  [cands]
  [:table.pure-table
   [:thead
    [:tr 
     [:th "cipherlen"] 
     [:th "candidate"] 
     [:th "score"] 
     [:th "head of message"] 
     [:th "x->cipher y->ref (vert. green lines are good matches)"]
     ;; [:th "top 3 pos"]
     ]]
     (for [{:keys [cipher-len best-cipher best-cipher-score head-msg cands mats]} cands]
       ^{:key (keyword "index" best-cipher)}
       [:tbody
        [:tr 
         [:td cipher-len] 
         [:td best-cipher] 
         [:td (format "%.1f" best-cipher-score)] 
         [:td head-msg] 
         [:td 
          (let [c best-cipher
                is (range cipher-len) ]
            
            (for [i is] 
              ^{:key (keyword "index" (str c "-" i) )}
              [imats (str c "-" i) (get mats i)]))]
         ;; [:td (str (map #(take 3 %) cands))]
         ]]
       )
])

(defn ^:export crack-component []
  (let [state @ui-state]
     [:div [:p "Type or paste a message select en/decrypt and click crypt"]
         [:form 
           {:on-submit (fn [event] (.preventDefault event))}
           [:div
            [:div 
             [:label "encrypted message (bg english)"]
             [:br]
             [:textarea {:cols 80 :rows 12
                      :value (get state :encr)
                      :on-change (ui-state-updater [:encr])}]]
            [:div
             [:label "word length"]
             [:input {:type "text"
                      :value (get state :cr-cl)
                      :on-change (ui-state-updater [:cr-cl])}]
             ]
            [:div
             [:button {:on-click (fn [ev] (analyze))} "analyze"]]
            ]]
      [:div
       [:h3 "result:"]
       [result-component (get state :candidates)]]
]))

(defn ^:export crypt-component []
  (let [state @ui-state]
     [:div [:p "Paste a message and run analysis to see decipher candidates and stats"]
         [:form.pure-form 
           {:on-submit (fn [event] (.preventDefault event))}
           [:div
             [:label "Message to encrypt (or decrypted message)"]
 [:br]
             [:textarea {:cols 80
                      :rows 14
                      :value (get state :message)
                      :on-change (ui-state-updater [:message])}]
            [:div
             [:button {:on-click (fn [ev] (encrypt-message))} "↓ encrypt ↓"]
             [:label "Cipher: "]
             [:input {:type "text"
                      :value (get state :cipher)
                      :on-change (ui-state-updater [:cipher])}]
             
             [:button {:on-click (fn [ev] (decrypt-message))} "↑ decrypt ↑"]
             ]
             [:label "encrypted message past here if you want to decrypt"]
            [:br]
             [:textarea {:cols 80
                      :rows 12
                      :value (get state :encr)
                      :on-change (ui-state-updater [:encr])}]
             ]]]     
))

;; -------------------------
;; Views

(defn home-page []
  [:div#main [:h2 "Welcome to clojurebridge-cipher-crack"]
   [:div [:a {:href (about-path)} "go to about page"]
         [:a {:href (crypt-path)} "en/decrypt a message with vinegre"]]
         [:a {:href (crack-path)} "take a try cracking a vinagre encrypted message"]]
)

(defn about-page []
  [:div#main [:h2 "About clojurebridge-cipher-crack"]
   [:div [:a {:href (index-path)} "go to the home page"]]
   [:div [:h3 "solution to Part3 of the clojurebridge-cipher"]
         [:a {:href "https://www.meetup.com/__ms195702012/Boston-Clojure-Group/events/238442827/?rv=cr2&_xtd=gatlbWFpbF9jbGlja9oAJGM4YjQ5ODdmLWRhMWMtNGFjMS1hYWI1LThlZDAxN2IwOGU3Ng&_af=event&_af_eid=238442827&expires=1492206992079&sig=98c8d82a3e6b01ffe42cf8c2d744adfc7b8c3240"} "boston clojure meetup April"]
         [:pre v/about ]]] )

(defn crypt-page []
  (let [state @ui-state]
    [:div#main
     [:div.header 
      [:h3 "Encrypt or decrypt a message with vinagre cipher"]
       [:ul.pure-menu-horizontal
        [:li.pure-menu-list [:a {:href (crack-path)} "take a try cracking a Vigenere encrypted message"]]
        [:li.pure-menu-separator ]
        [:li.pure-menu-list  [:a {:href (about-path)} "go to about page"]]]

      ]
     [:div.content
     
       [crypt-component]    
     ]]))

(defn crack-page []
  (let [state @ui-state]
    [:div#main
     [:div.header 
      [:h3 "Vigenere-cipher: comparing letter frequencies to a reference for english"]
      [:ul.pure-menu-horizontal
        [:li.pure-menu-list [:a {:href (index-path)} "en/decrypt a message with Vigenere"]
        [:li.pure-menu-separator ]
        [:li.pure-menu-list  [:a {:href (about-path)} "go to about page"]]]]
      [:div.content
      [crack-component]    
     ]]]))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "index.html" []
  (session/put! :current-page #'crypt-page))

(secretary/defroute index-path "/" []
  (session/put! :current-page #'crypt-page))

(secretary/defroute about-path "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute crypt-path "/crypt" []
  (session/put! :current-page #'crypt-page))

(secretary/defroute crack-path "/crack" []
  (session/put! :current-page #'crack-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn ^:export main []
  (enable-console-print!)
  (dirac.runtime/install!)
  (secretary/set-config! :prefix "#")
  ;; Attach event listener to history instance.
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))))
    (.setEnabled history true))

  ;; Accountant interferes with secretarys # prefix which allows it to be deployed to a filesystem

  ;; (accountant/configure-navigation!
     ;; {:nav-handler
      ;; (fn [path]
        ;; (secretary/dispatch! path))
      ;; :path-exists?
      ;; (fn [path]
        ;; (secretary/locate-route path))})
   ;; (accountant/dispatch-current!)
  (mount-root) )

(main)
