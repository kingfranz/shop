(defproject shop2 "0.7.3"
  	:description "Shopping list"
  	:url "http://soahojen.se"
  	:license {:name "Eclipse Public License"
              :url "http://www.eclipse.org/legal/epl-v10.html"}
  	:dependencies [
        [org.clojure/clojure "1.9.0-alpha17"]
  		[hiccup "1.0.5"]
  		[ring "1.5.0"]
       	[org.clojure/spec.alpha "0.1.123"]
        [ring/ring-defaults "0.2.3"]
        [clj-time "0.13.0"]
        [environ "1.1.0"]
        [com.novemberain/monger "3.1.0"]
        [garden "1.3.2"]
        [cheshire "5.7.1"]
        [ring-logger "0.7.7"]
        [com.cemerick/friend "0.2.3"]
        [com.taoensso/timbre "4.10.0"]
        [compojure "1.5.2"]]
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
        :dev {:uberjar {:aot :all}}})
