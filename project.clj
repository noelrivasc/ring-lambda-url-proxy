(defproject ring-lambda-url-proxy "0.1.0"
  :description "Ring middleware for AWS Lambda Function URL requests and responses"
  :url "https://github.com/noelrivasc/ring-lambda-url-proxy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :profiles {:dev {:dependencies [[cheshire "5.7.0"]
                                  [compojure "1.5.2"]
                                  [ring/ring-json "0.4.0"]]}})

