(ns segment.core
  (:require [hickory.core :as hickory]
            [hickory.zip :refer [hickory-zip]]
            [clojure.zip :as zip]
            [clojure.string :as str]
            [clojure.data.zip :as dzip]
            [segment.road-name :refer [road-name]]))

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
;; Extraction functions
;;-------------------------------------

(defn- content
  "Extracts all text (recursively) in this tree"
  [accum loc]
  (let [old (get accum :content "")
        new (if (string? (zip/node loc))
              (str old (zip/node loc))
              old)]
    (assoc accum :content new)))

(defn- height
  "Extracts the maximum height of this tree"
  [accum loc]
  (let [old (get accum :height 0)
        new (max old (count (zip/path loc)))]
    (assoc accum :height new)))

(defn- root-tag
  "Extracts the tag at the root of this tree"
  [loc]
  {:tag (name (get (zip/node loc) :tag ""))})

(defn- depth
  "Extracts the depth of this tree"
  [loc]
  {:depth (count (dzip/ancestors loc))})

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
  "Cleans a page by removing comments and some tags that don't carry content.
  Returns a subtree rooted at the :body tag"
  [page-loc]
  (->> (page-body page-loc)
       (zip-remove #(= :comment (:type %)))
       (zip-remove (fn [node]
                     (#{:meta :link :script :style :img :noscript :path :hr :br :input}
                      (:tag node))))))

(defn- loc-extract
  "Applies extraction fns on loc.

  These extraction functions do not need to go down the entire tree"
  [fns loc]
  (apply merge ((apply juxt fns) loc)))

(defn- tree-extract
  "Applies extraction fns on a subtree rooted at loc.
  Will reduce over the entire subtree"
  ([reducers loc] (tree-extract reducers {} loc))
  ([reducers accum loc]
   (if (zip/end? loc) accum
       (recur reducers
              (reduce (fn [a r] (r a loc)) accum reducers)
              (zip/next loc)))))

(defn- extract
  "Applies loc-extract and tree-extract, and merges all the data together.

  Assumes that all extraction functions in loc-extract and tree-extract
  do not have conflicting keys"
  [loc]
  (apply merge
         ((juxt (partial loc-extract [root-tag depth])
                (partial tree-extract [content height]))
          loc)))

(defn- content-extract
  [{:keys [content] :as m}]
  (assoc m :road-name (road-name content)))

(defn- segments*
  "Return segments from a page zipper"
  [page-zipper]
  (let [descendants (->> page-zipper
                         page-clean
                         dzip/descendants)
        subtrees (map zip-subtree descendants)
        extracted (map (comp content-extract extract) subtrees)]
    extracted))

(defn segments
  "Return segments (vector), given a html page as a string.

  A segment is a 'row' of data, in the following form
  { :tag :p, :depth 1, :content \"It only has two paragraphs\":height 1 }

  Each field is extracted using `extract` (above)"
  [page]
  (-> page
      hickory/parse
      hickory/as-hickory
      hickory-zip
      segments*))


(comment
  (def page (slurp "test/segment/data/test.html"))

  (def page-zip (-> page
                    hickory/parse
                    hickory/as-hickory
                    hickory-zip))

  (map zip/node (-> page-zip
                    dzip/descendants
                    (nth 4)
                    dzip/ancestors))

  (segments page)

  (extract page-zip)
  )

(defn- cleanup
  [s]
  (-> s
      (str/replace #"[\t\r\n]" " ")
      (str/replace #"\s+" " ")
      str/trim))

(defn distinct-by
  "Returns a lazy sequence of the elements of coll with duplicates removed.
  Returns a stateful transducer when no collection is provided."
  ([coll f compare]
   (let [step (fn step [xs seen]
                (lazy-seq
                 ((fn [[f :as xs] seen]
                    (when-let [s (seq xs)]
                      (if (contains? seen f)
                        (recur (rest s) seen)
                        (cons f (step (rest s) (conj seen f))))))
                  xs seen)))]
     (step coll #{}))))
