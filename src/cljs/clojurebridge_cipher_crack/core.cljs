(ns clojurebridge-cipher-crack.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [clojurebridge_cipher_crack.vinegre :as v]
              [goog.string :as gstring]
              [goog.string.format]
              [goog.events :as events])
              (:import goog.History))

(defn format
  [fmt & args]
  (apply gstring/format fmt args))

;; state
(defonce ui-state (reagent/atom {:message "Put your message here. All spaces and non-letters will be stripped. Charakters lowercased, before en/decrypting. You probably need a message of at least thousend charakters to crack a message with a cipher of lenght 5 or more. The background (letter frequencies) are for english only, you will have to change the source if you want to crack a message in another langugage. By default the cracking page will take the message plain if decrypt is selected or encryted if the encrypt option is selected. Enjoy.Put your message here. All spaces and non-letters will be stripped. Charakters lowercased, before en/decrypting. You probably need a message of at least thousend charakters to crack a message with a cipher of lenght 5 or more. The background (letter frequencies) are for english only, you will have to change the source if you want to crack a message in another langugage. By default the cracking page will take the message from below (encrypted) to save you cut&paste. The scoring function does not always compare different cipher lengths well." :cipher "clojurebridge" :encrypt true :cr-cl "1 2 3 4 5 6 7 8 9 10 11 12 13 14 15"}))

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
(defn ^:export draw [id mdata]
  (let [cnv (.getElementById js/document id)
        ctx (.getContext cnv "2d")
        img-data (.getImageData ctx 0 0 26 26)
        data (.-data img-data)]
    (.log js/console  (str "draw:  " id " head: arg.data:" (take 10 mdata)))
  (doall (for [i (range 2704)] (aset data i (js/Number (get mdata i)))))
  (.putImageData ctx img-data 0 0)
  (.log js/console  (str "draw:  " id " head image.data: " (aget data 0) (aget data 1) (aget data 2) (aget data 3)   ))))
  

(defn analyze [] (swap! ui-state assoc-in [:candidates]
                        (v/candidates (nrange (get @ui-state :cr-cl)) (get @ui-state :encr))))
;(u/candidates [3 4 5] (get @ui-state :encr))))

(defn mat2img-data [m]
 (let [ flat (flatten (first m))
        scaled (mapv #(* 10) flat)
        zero (map #(* 0 %) flat)
        image-data (interleave scaled zero zero)]
  image-data
))

;; components
;; (defn imats [id mats]
;;   (let [mid (str "mat-" id)
;;         flat (flatten (first mats))
;;         zero (map #(* 0 %) flat)
;;         image-data (interleave flat zero zero)]
;;   [:canvas {:id mid :width "26" :height "26" :after-render #(draw mid image-data)}]
;; ))

(defn ^:export imats [id mat]
  (let [mid (str "mat-" id)
        flat (flatten mat)
        sc   10.0
        sc-r (mapv #(- (* sc %) 320) flat)
        sc-g (mapv #(- 255 (* sc %)) flat)
        n    (count flat)
        zero (vec (repeat n 0))
        fill (vec (repeat n 255))
        image-data (vec (interleave sc-r sc-g zero fill))]
    (reagent/create-class
     {:component-did-mount #(draw mid image-data)
      :reagent-render (fn [] [:canvas {:id mid :width "26" :height "26"} ] ) })
))

(defn ^:export result-component [cands]
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
             [:button {:on-click (fn [ev] (analyze))} "analyse"]]
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
                      :rows 12
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
        [:li.pure-menu-list [:a {:href (crack-path)} "take a try cracking a vinagre encrypted message"]]
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
      [:h3 "vinegre-cipher: comparing letter frequencies to a reference for english"]
      [:ul.pure-menu-horizontal
        [:li.pure-menu-list [:a {:href (index-path)} "en/decrypt a message with vinegre"]
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
  (secretary/set-config! :prefix "#")
  ;; Attach event listener to history instance.
  (let [history (History.)]
    (events/listen history "navigate"
                   (fn [event]
                     (secretary/dispatch! (.-token event))))
    (.setEnabled history true))

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
