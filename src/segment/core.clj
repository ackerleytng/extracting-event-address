(ns segment.core
  (:require [hickory.core :as hickory]
            [hickory.zip :refer [hickory-zip]]
            [clojure.zip :as zip]
            [clojure.string :as str]
            [clojure.data.zip :as dzip]))

;;-------------------------------------
;; Augmenting (clojure|hickory).zip
;;-------------------------------------

(defn zip-remove
  "Remove all nodes from this tree where pred.
  Returns a new zipper at the root"
  [pred loc]
  (cond
    (zip/end? loc) (hickory-zip (zip/root loc))
    (pred (zip/node loc)) (recur pred (zip/remove loc))
    :else (recur pred (zip/next loc))))

(defn zip-subtree
  "Returns the subtree rooted at loc as a new zipper"
  [loc]
  (hickory-zip (zip/node loc)))

;;-------------------------------------
;; Segment functions
;;-------------------------------------

(defn- first-descendant
  "Get the first descendant with a :tag matching tag"
  [loc tag]
  (->> (dzip/descendants loc)
       (filter #(= tag (:tag (zip/node %))))
       first))

(defn- page-body
  "Returns the body subtree in this html as a new zipper"
  [loc]
  (zip-subtree (first-descendant loc :body)))

(defn- page-clean
  "Cleans a page by removing comments and some tags that don't carry content"
  [page-loc]
  (->> (page-body page-loc)
       (zip-remove #(= :comment (:type %)))
       (zip-remove (fn [node] (#{:meta :link :script :style} (:tag node))))))

(defn- content
  "Returns the entire content of a tree, given the tree at its root"
  ([loc] (content loc ""))
  ([loc accum]
   (if (zip/end? loc) accum
       (recur (zip/next loc)
              (if (string? (zip/node loc))
                (str accum (zip/node loc))
                accum)))))

(defn- cleanup
  [s]
  (-> s
      (str/replace #"[\t\r\n]" " ")
      (str/replace #"\s+" " ")))

(defn- segments*
  "Return segments from a page zipper"
  [page-zip]
  (let [descendants (->> (page-body page-zip)
                         page-clean
                         dzip/descendants)]
    (set (map (comp cleanup content zip-subtree) descendants))))

(defn segments
  "Return segments, given a html page as a string"
  [page]
  (let [page-zip (hickory-zip (hickory/as-hickory (hickory/parse page)))]
    (segments* page-zip)))

(comment
  (def page (slurp "data/files/11-budget-buffets-in-singapore-20-and-below.html"))
  (filter (fn [s] (.startsWith s "Address")) (segments page)))
