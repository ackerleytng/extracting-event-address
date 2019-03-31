(defproject segment "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [;; Sticking with older version of clojure because compilation to native
                 ;;   doesn't seem to be working yet for 1.10
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/data.zip "0.1.3"]
                 [org.clojure/data.csv "0.1.4"]
                 [org.clojure/tools.cli "0.4.1"]
                 [hickory "0.7.1"]]
  :main ^:skip-aot segment.cli
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :native-image
                       {:jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                        :opts ["--enable-url-protocols=http,https"
                               ;; Not safe, but it's triggering some issue with java.lang.ClassLoader.defineClass
                               ;;   that I don't know how to fix now
                               "--report-unsupported-elements-at-runtime"
                               "-H:ReflectionConfigurationFiles=reflection.json"]}}})
