(ns segment.cli
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [segment.core :as segment]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn segments
  [s options]
  (let [raw (segment/segments s)]
    (if (:raw options) raw
        (into []
              (comp (filter #(> (count %) 3))
                    (filter #(re-find #"[A-Za-z]" %)))
              raw))))

(defn csv!
  [file options]
  (let [out-file (:output options)
        append (if (contains? options :append)
                 (:append options)
                 (.exists (io/file out-file)))
        page (slurp file)
        segs (segments page options)
        rows (map vals segs)]
    (with-open [writer (io/writer out-file :append append)]
      (csv/write-csv writer
                     (if append rows
                         (let [header (map (comp #(str/replace % "-" "_") name) (keys (first segs)))]
                           (concat [header] rows)))))))

(comment
  (csv! "./data/files/11-budget-buffets-in-singapore-20-and-below.html" {:output "out.csv" :raw true :append false})
  (csv! "./test/segment/data/test.html" {:output "out.csv" :raw true :append false})

  (let [files (filter #(.isFile %) (file-seq (io/file "data/files")))]
    (println "Segmenting" (count files) "files")
    (doseq [f files]
      (println f)
      (csv! f {:output "out.csv" :raw true}))))

(def cli-options
  ;; An option with a required argument
  [["-o" "--output OUTPUT" "Output file"
    :default "out.csv"]
   [nil "--raw" "Dump raw segments. (We try to eliminate useless segments by default)"]
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
      {:file (first arguments) :options options})))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [file options raw exit-message ok?]} (validate args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (csv! file options))))
