(ns clojurebridge-cipher-crack.vinegre
  (:require [clojure.string]
            [clojure.set]))

;; https://github.com/clojurebridge-boston/track2-ciphers/blob/master/docs/track2-vigenere.md

(def alphabeth "abcdefghijklmnopqrstuvwxyz")

(def cipher "helloworld")

(def text "Part 3: Vigenere challenge Here is an example for you to try to break. The key is between two and eight characters long. It is a word and is related to the topic of this workshop. Be patient, this may take time. Try to automate you process as much as possible. We also recommend that you work in pairs.")

(defn strip-non-alpha [s]
  (apply str (re-seq #"[a-z]" (clojure.string/lower-case s)))
)

(defn e-map 
    "create encryption map for a cipher s"
    [s]
  (vec (for [c s] 
    (apply hash-map (interleave 
               (mapv identity alphabeth) 
               (vec (take 26 (drop-while #(not= c %) (cycle alphabeth))))
               ))))
)

(defn i-map
  "invert a en/decryption map m"
  [m]
  (mapv clojure.set/map-invert m))

(defn d-map [s] (i-map (e-map s)))

(defn vinegre-crypt
  "en/decryt string s using en/decryption map m"
  [m s]
  (apply str (map get (cycle m) (mapv identity s)))
)

(def txt (strip-non-alpha text))
(def vtxt (vinegre-crypt (e-map "abcd") txt))

(= txt (vinegre-crypt (d-map "abcd") (vinegre-crypt (e-map "abcd") txt)))

;; now to the cracking part

;; complete decrypt map for the alphabeth that we will use for each position to do freq plots and analysis
(def c-map (d-map alphabeth)) 

;; frequencies for english lang 
(def eng {
\e 12.02
\t 9.1
\a 8.12
\o 7.68
\i 7.31
\n 6.95
\s 6.28
\r 6.02
\h 5.92
\d 4.32
\l 3.98
\u 2.88
\c 2.71
\m 2.61
\f 2.3
\y 2.11
\w 2.09
\g 2.03
\p 1.82
\b 1.49
\v 1.11
\k 0.69
\x 0.17
\q 0.11
\j 0.1
\z 0.07})

;### Part 3: Vigenere challenge 
;Here is an example for you to try to break. The key is between two and eight characters long. It is a word and is related to the topic of this workshop.

(def encr3 (remove #(= \newline %) "rzsrppgeamjllagcpwxismxxcalecwygluetcaguepwwlznpclepcsgcpkgbac
ltcifstvntybwsepwutzkinweettwgqwjpnweefbwgazgvciebtvyalvyjlowh
smhdacdpcqrtobzttlwpznepnpacpqfspxwcomfiazgvciebtvyalvyjlowhhp
arstwsewlwplwkptgexmfiznudmwddymguepwutzkisqywwmntwxjdrzsbxqfv
wifvfiytdwoxyoldsmjpnkgbatahsuwceascopwgyinpwzscvazthikhzuwitu
whcmxtczwsewshlusotvyvciutepwjdvskjijapqywmcjzpkdpdayjtlwsxqkh
ttwspalgzgwgfakwzxhtceshyietonggsmjhsmopdxghepmbzckajiopclwsep
wecmkxomfitidbplsaznxgpmvdxjqecmkxomfimijpnsgqlus"))

(def about "
;; not sure if that is the best way but here is the plan
;; we want a matrix
;;   a b c d <-cipher char for pos
;; a 
;; b   squared diff to eng lang freq
;; c
;; ^-alphabeth
;;
;; plot as img
;; 
;; get geom average for each positional candidate
;;
;; make a lazy list that chooses the next cipher candidate by lowest score
;; 
;; take as many as it takes to crack the message
;;
;; cljs gui with plots 
")

(def transpose (partial apply mapv vector))

;; convinience for repl
(defn chunks-by-cipher-len  
  "input: cipher-lenght cl , string s"
  [cl s]
  (let [el (/ (count encr3) 100.0 cl)
        pe (mapv vec (partition cl s))]
    (transpose pe)
))

 

;; we want a matrix
;;   a b c d <-cipher char for pos
;; a 
;; b   squared diff to eng lang freq
;; c
;; ^-alphabeth

(defn mat-freq-var
  "in: rf ref-freqs, s string-for-a-pos"
  [rf s]
   (let [l (count s)
         is (range (count alphabeth))
         nu (fn [n] (if (nil? n) 0 n))
         cs (mapv identity s)
         fch (fn [a b] (let [d (- (nu a) (nu b))]  (* d d) ))
         freqs-by-i (vec (for [i is] (frequencies (map #(get (c-map i) %) cs))))
         ] 
   ;  freqs-by-i
     (for [cy alphabeth] 
       (vec (for [x is] (fch (get rf cy) (/ (* 100 (get (get freqs-by-i x) cy)) l))))
     )))

;;
;; plot as img
;; 

;; get  average for each positional candidate
;;
(defn keys-and-avg 
  [mat]
  (let [sum (mapv #(apply + %) (transpose mat))
        abc  (mapv identity alphabeth)]
  (sort-by second < (mapv vec (partition 2 (interleave abc sum)
)))))

;; make a lazy list that chooses the next cipher candidate by lowest score
;; 
(defn candidates
  "cls cipher-lengths return a list"
  [cls s]
  (sort-by :best-cipher-score < 
   (vec (for [cl cls] 
    (let [chks (chunks-by-cipher-len cl s)
         ; cands (for [pos (range cl)] (keys-and-avg (mat-freq-var eng (get chks pos))))
          mats (vec (for [pos (range cl)] (vec (mat-freq-var eng (get chks pos)))))
          cands (map keys-and-avg mats)
          best-cipher  (apply str (map first (map first cands)))]
        {:cipher-len cl
         :best-cipher best-cipher
         :best-cipher-score (/ (apply + (map second (map first cands))) cl) 
         :cands cands
         :head-msg (take 40 (vinegre-crypt (d-map best-cipher) s))
         :mats mats
      ;   :best5-per-pos-w-score (map #(take 5 %) cands) 
        }
)))))

;; (candidates (range 2 9) encr3)          

;; take as many as it takes to crack the message
;;
;; cljs gui with plots 
