(ns clojurebridge-cipher-crack.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [clojurebridge_cipher_crack.util :as u]))

;; state
(defonce ui-state (reagent/atom {:message "Put your message here. All spaces and non-letters will be stripped. Charakters lowercased, before en/decrypting. You probably need a message of at least thousend charakters to crack a message with a cipher of lenght 5 or more. The background (letter frequencies) are for english only, you will have to change the source if you want to crack a message in another langugage. By default the cracking page will take the message plain if decrypt is selected or encryted if the encrypt option is selected. Enjoy." :cipher "clojurebridge" :encrypt true}))

;; event functions
(defn crypt-message [] (swap! ui-state assoc-in [:result]
                                       (u/vinegre-crypt 
                                        (if (get @ui-state :encrypt) 
                                          (u/e-map (get @ui-state :cipher)) 
                                          (u/d-map (get @ui-state :cipher)))
                                        (u/strip-non-alpha (get @ui-state :message)))))
(defn ui-state-updater [path]  (fn [ev] (swap! ui-state assoc-in path (.-value (.-target ev)))))
(defn ui-state-toggle  [path]  (swap! ui-state update-in path not))


;; components
(defn crypt-component []
  (let [state @ui-state]
     [:div [:p "Type or paste a message select en/decrypt and click crypt"]
         [:form 
           {:on-submit (fn [event] (.preventDefault event))}
           [:p
             [:label "Message"]
             [:input {:type "textarea"
                      :value (get state :message)
                      :on-change (ui-state-updater [:message])}]
             [:label "Cipher"]
             [:input {:type "text"
                      :value (get state :cipher)
                      :on-change (ui-state-updater [:cipher])}]
             [:label "encrypt (otherwise decrypt)"]
             [:input.toggle {:type "checkbox" 
                            :checked (get state :encrypt)
                            :on-change #(ui-state-toggle [:encrypt])}] 
             [:button {:on-click (fn [ev] (crypt-message))} "crypt"]]]
      [:div
       [:h3 "result:"]
       [:p (get state :result)]]
]))



;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to clojurebridge-cipher-crack"]
   [:div [:a {:href "/about"} "go to about page"]
         [:a {:href "/crypt"} "en/decrypt a message with vinegre"]]
         [:a {:href "/crack"} "take a try cracking a vinagre encrypted message"]]
)

(defn about-page []
  [:div [:h2 "About clojurebridge-cipher-crack"]
   [:div [:a {:href "/"} "go to the home page"]]
   [:div [:h3 "solution to Part3 of the clojurebridge-cipher"]
         [:a {:href "https://www.meetup.com/__ms195702012/Boston-Clojure-Group/events/238442827/?rv=cr2&_xtd=gatlbWFpbF9jbGlja9oAJGM4YjQ5ODdmLWRhMWMtNGFjMS1hYWI1LThlZDAxN2IwOGU3Ng&_af=event&_af_eid=238442827&expires=1492206992079&sig=98c8d82a3e6b01ffe42cf8c2d744adfc7b8c3240"} "boston clojure meetup April"]
         [:pre u/about ]]] )

(defn crypt-page []
  (let [state @ui-state]
    [:div [:h2 "Encrypt or decrypt a message with vinagre cipher"]
     [:div [:a {:href "/"} "go to the home page"]]
      [crypt-component]    
     ]))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

(secretary/defroute "/crypt" []
  (session/put! :current-page #'crypt-page))

(secretary/defroute "/crack" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
