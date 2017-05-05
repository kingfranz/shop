(defproject shop2 "0.1.0"
  :description "Shopping list"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha15"]
  				 [hiccup "1.0.5"]
  				 [ring "1.5.0"]
                 [ring/ring-defaults "0.2.3"]
                 [clj-time "0.13.0"]
                 [com.novemberain/monger "3.1.0"]
                 [garden "1.3.2"]
                 [cheshire "5.7.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [compojure "1.5.2"]]
  :main ^:skip-aot shop2.core
  :target-path "target/%s"
  :uberjar-name "shop-standalone.jar"
  :plugins [[lein-ring "0.11.0"]]
  :ring {:handler shop2.core/application
  		 :auto-reload? true
         :auto-refresh? true
         :ssl? true
         :ssl-port 9443
         :keystore "./keystore"
         :key-password "Benq.fp731"}
  :profiles {:dev {:uberjar {:aot :all}}})
