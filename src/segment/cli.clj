(ns segment.cli
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [segment.core :refer [segments]]
            [clojure.tools.cli :refer [parse-opts]])
  (:gen-class))

(defn csv!
  [file options]
  (let [out-file (:output options)
        append (if (contains? options :append)
                 (:append options)
                 (.exists (io/file out-file)))
        segs (segments (slurp file))
        rows (map vals segs)]
    (with-open [writer (io/writer out-file :append append)]
      (csv/write-csv writer
                     (if append rows
                         (let [header (map (comp #(str/replace % "-" "_") name) (keys (first segs)))]
                           (concat [header] rows)))))))

(defn convert
  [path options]
  (let [^java.io.File p (io/file path)]
    (cond
      (.isFile p)
      (do (println "Segmenting" path)
          (csv! path options))
      (.isDirectory p)
      (let [files (filter #(.isFile %) (file-seq p))]
        (println "Segmenting" (count files) "files:")
        (doseq [f files]
          (println "  +" (.getPath f))
          (csv! f options)))
      :else
      (println "Invalid path" path))))

(comment
  (csv! "./data/files/11-budget-buffets-in-singapore-20-and-below.html" {:output "out.csv" :append false})
  (csv! "./test/segment/data/test.html" {:output "out.csv" :append false})

  (convert "data/files" {:output "out.csv"})
  )

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
        "  input    path to input file (html page) or directory containing html files"]
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
      {:path (first arguments) :options options})))

(defn- exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [{:keys [path options exit-message ok?]} (validate args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (convert path options))))
