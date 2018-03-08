(defproject shop2 "0.7.6"
  	:description "Shopping list"
  	:url "http://soahojen.se"
  	:license {:name "Eclipse Public License"
              :url "http://www.eclipse.org/legal/epl-v10.html"}
  	:dependencies [
        [org.clojure/clojure "1.9.0"]
  		[hiccup "1.0.5"]
  		[ring "1.6.2"]
       	[org.clojure/spec.alpha "0.1.123"]
        [ring/ring-defaults "0.3.1"]
        [clj-time "0.14.0"]
        [environ "1.1.0"]
        [com.novemberain/monger "3.1.0"]
        [prone "1.1.4"]
        [garden "1.3.2"]
        [cheshire "5.8.0"]
        [ring-logger "0.7.7"]
        [com.cemerick/friend "0.2.3"]
        [com.taoensso/timbre "4.10.0"]
        [compojure "1.6.0"]]
  	:main ^:skip-aot shop2.core
  	:target-path "target/%s"
  	:jvm-opts ["-Dclojure.spec.compile-asserts=true"]
  	:plugins [
        [lein-ring "0.11.0"]
  		[lein-pprint "1.1.2"]
     	[lein-environ "1.1.0"]]
  	:ring {
        :handler shop2.core/application
  		:auto-reload? true
        :auto-refresh? false
        :port 3000}
  	:profiles {
        :dev {
            :uberjar {:aot :all}
            :ring {:stacktrace-middleware prone.middleware/wrap-exceptions}}})
