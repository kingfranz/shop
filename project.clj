(defproject shop2 "1.10.1"
    :description "Shopping list"
    :url "http://soahojen.se"
    :license {:name "Eclipse Public License"
              :url  "http://www.eclipse.org/legal/epl-v10.html"}
    :dependencies [
                   [org.clojure/clojure "1.9.0"]
                   [hiccup "1.0.5"]
                   [ring/ring-core "1.6.3"]
                   [org.clojars.kingfranz/utils "0.2.5"]
                   [org.clojure/spec.alpha "0.1.143"]
                   [orchestra "2017.11.12-1"]
                   [http-kit "2.2.0"]
                   [ring/ring-json "0.4.0"]
                   [ring-logger-timbre "0.7.6"]
                   [ring/ring-spec "0.0.4"]
                   [ring/ring-anti-forgery "1.2.0"]
                   [ring/ring-codec "1.1.0"]
                   [metosin/ring-http-response "0.9.0"]
                   [slingshot "0.12.2"]
                   [clj-time "0.14.2"]
                   [environ "1.1.0"]
                   [com.novemberain/monger "3.1.0"]
                   [prone "1.5.1"]
                   [garden "1.3.5"]
                   [cheshire "5.8.0"]
                   [com.cemerick/friend "0.2.3"]
                   [com.taoensso/timbre "4.10.0"]
                   [compojure "1.6.0"]]
    :main shop2.core
    :aot [shop2.core]
    ;:main ^:skip-aot shop2.core
    :uberjar-name "shopping.latest.jar"
    :target-path "target/%s"
    :jvm-opts ["-Dclojure.spec.compile-asserts=true"]
    :plugins [
              [lein-ring "0.11.0"]
              [lein-pprint "1.1.2"]
              [lein-environ "1.1.0"]]
    :profiles {:uberjar {:aot :all}
               :dev {:ring {:stacktrace-middleware prone.middleware/wrap-exceptions}}}
    )

; {:jvm-opts ["-Dclojure.spec.compile-asserts=true -XX:-OmitStackTraceInFastThrow"]}
