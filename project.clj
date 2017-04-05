(defproject shop2 "0.1.0"
  :description "Shopping list"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
  				 [hiccup "1.0.5"]
  				 [lein-kibit "0.1.3" :exclusions [org.clojure/tools.cli org.clojure/clojure]]
  				 [ring-middleware-format "0.7.2" :exclusions [commons-codec]]
  				 [ring/ring-anti-forgery "1.0.1"]
                 [ring-webjars "0.1.1"]
                 [ring/ring-defaults "0.2.3"]
                 [ring/ring-core "1.6.0-RC1"]
                 [ring/ring-devel "1.6.0-RC1"]
                 [ring/ring-jetty-adapter "1.6.0-RC1"]
                 [clj-time "0.13.0"]
                 [garden "1.3.2"]
                 [compojure "1.5.2"]]
  :main ^:skip-aot shop2.core
  :target-path "target/%s"
  :uberjar-name "shop-standalone.jar"
  :plugins [[lein-ring "0.11.0"]]
  :ring {:handler shop2.core/application
  		 :auto-reload? true
         :auto-refresh? true}
  :profiles {:dev {:uberjar {:aot :all}}})
