(ns segment.road-name
  (:require [clojure.string :as str]))

(def ^:private malay-generic-element
  ["bukit" "jalan" "kampong" "lengkok" "lengkong" "lorong" "padang" "taman" "tanjong"])

(def ^:private malay-abbr
  ["bt" "jln" "kg" "lor" "tg"])

(def ^:private english-prefixes
  ["sector" "mount"])

(def ^:private english-prefixes-abbr
  ["mt"])

(def ^:private english-suffixes
  ["alley" "avenue" "bank" "boulevard" "bow" "central" "circle" "circuit" "circus"
   "close" "concourse" "court" "crescent" "cross" "crossing" "drive" "east"
   "estate" "expressway" "farmway" "field" "garden" "gardens" "gate" "gateway"
   "grande" "green" "grove" "heights" "height" "highway" "hill" "island"
   "junction" "lane" "link" "loop" "mall" "north" "park" "parkway" "path" "place"
   "plain" "plains" "plaza" "promenade" "quay" "ridge" "ring" "rise" "road"
   "sector" "south" "square" "street" "terrace" "track" "turn" "vale" "valley"
   "view" "vista" "walk" "way" "west" "wood"])

(def ^:private english-suffixes-abbr
  ["ave" "blvd" "cl" "cres" "dr" "e'way" "hway" "pl" "rd" "sq" "st"])

(defn- sort-longest-first
  [v]
  (sort #(compare (count %2) (count %1)) v))

(def ^:private prefixes
  (->> (concat malay-generic-element malay-abbr english-prefixes english-prefixes-abbr)
       ;; sort to allow regex to get the longest possible match
       sort-longest-first))

(def ^:private suffixes
  (->> (concat english-suffixes english-suffixes-abbr)
       sort-longest-first))

(def ^:private special-cases
  (sort-longest-first
   [;; Road names without any generic element
    "geylang bahru"
    "geylang serai"
    "kallang bahru"
    "kallang tengah"
    "wholesale centre"
    ;; Road names with the definite article "the"
    "the inglewood"
    "the knolls"
    "the oval"
    ;; Road names that consist of a single word
    "bishopsgate"
    "causeway"
    "piccadilly"
    "queensway"]))

(def ^:private spaces-regex
  "[ \\t]")

(def ^:private number-suffix-regex
  "For cases like Ang Mo Kio Ave* 1*"
  (str "(?:" spaces-regex "+\\d+)"))

(def ^:private word-regex
  "For words that can appear in road names, like '*Laurel* *Wood* Avenue' or
  '*one-north* Gateway' or '*Saint* *Anne's* Wood'"
  "[\\w-']+")

(def ^:private suffix-regex
  (re-pattern
   (str "(?i)"  ; set case insensitive matching
        ;; word that does not contain numbers (to avoid getting the house number in the road name)
        ;;   like *12* Collyer Quay
        "(?:[a-z-']+" spaces-regex "+)"
        "(?:" word-regex spaces-regex "+){0,3}"  ; words that come before the suffix (up to 3)
        "(?:" (str/join "|" suffixes) ")"  ; the suffix itself
        number-suffix-regex "?"  ; any numerical suffix
        )))

(def ^:private prefix-regex
  (re-pattern
   (str "(?i)"  ; set case insensitive matching
        "(?:" (str/join "|" prefixes) ")"  ; the prefix itself
        "(?:" spaces-regex "+" word-regex "){1,5}"  ; words that come after the suffix (up to 5)
        )))

(def ^:private special-cases-regex
  (re-pattern (str "(?i)" "(?:" (str/join "|" special-cases) ")")))

(defn road-name
  [string]
  (let [prefix-match (re-find prefix-regex string)
        suffix-match (re-find suffix-regex string)
        special-case-match (re-find special-cases-regex string)
        matches (filter identity [prefix-match suffix-match special-case-match])
        n-matches (count (filter identity matches))]
    (if (> n-matches 1)
      (first (sort-longest-first matches))
      (some identity matches))))
