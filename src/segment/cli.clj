(ns segment.cli
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [segment.core :as segment]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn csv!
  [file out-file]
  (let [page (slurp file)
        segments (segment/segments page)
        transposed (mapv vector segments)
        data (into [["segments"]] transposed)]
    (with-open [writer (io/writer out-file)]
      (csv/write-csv writer data))))

(def cli-options
  ;; An option with a required argument
  [["-o" "--output OUTPUT" "Output file"
    :default "out.csv"]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn- usage [options-summary]
  (->> ["Outputs segments of a html page to csv."
        ""
        "Usage: program-name [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Arguments:"
        "  input    path to input file (html page)"]
       (str/join \newline)))

(defn- error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn- validate
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options)
      {:exit-message (usage summary) :ok? true}
      errors
      {:exit-message (error-msg errors)}
      (not (= 1 (count arguments)))
      {:exit-message "Need one argument: the file to dump segments from"}
      (not (.exists (io/file (first arguments))))
      {:exit-message (str (first arguments) "does not exist!")}
      :else
      {:file (first arguments) :output (:output options)})))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [file output exit-message ok?]} (validate args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (csv! file output))))
