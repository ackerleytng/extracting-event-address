(defproject segment "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/data.zip "0.1.3"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/tools.cli "0.4.1"]
                 [hickory "0.7.1"]]
  :main ^:skip-aot segment.cli
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}
             :dev [:project/dev]
             :project/dev {:dependencies [[org.clojure/core.async "0.4.490"]]
                           :resource-paths ["/home/ackerleytng/installs/rebl/rebl.jar"]}})
