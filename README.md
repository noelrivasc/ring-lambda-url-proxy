# ring-lambda-url-proxy

Ring middleware for handling AWS Lambda Function URL requests and responses

This library is heavily based on [mhjort/ring-apigw-lambda-proxy](https://github.com/mhjort/ring-apigw-lambda-proxy) and adapted to work with AWS Lambda Function URLs instead of API Gateway Lambda proxy integration.

Note! UTF-8 is used as encoding everywhere. HTTP response contains always message-body, so using HEAD should be avoided.

## Installation

Add the following to your `project.clj` `:dependencies`:

```clojure
[noelrivasc/ring-lambda-url-proxy "0.1.0"]
```

## Usage

This library can be used to wrap AWS Lambda Function URL request and response
so that they can be used together with Ring. It takes AWS Lambda Function URL request
as input parameter (parsed from JSON with keywords) and creates Ring compatible
request map. Same way it transforms Ring response map to AWS Lambda Function URL response
map which can be marshaled to JSON back.

Note! This is not a standard Ring Middleware because this has to be always first
middleware in a chain.

AWS Lambda Function URL JSON Rest API example.

Add `ring/ring-core`, `ring/ring-json`,`compojure`,`lambdada` and `cheshire` dependencies.

```clojure
(ns example
  (:require [uswitch.lambada.core :refer [deflambdafn]]
            [cheshire.core :refer [parse-stream generate-stream]]
            [clojure.java.io :as io]
            [ring.middleware.lambda-url :refer [wrap-lambda-url-proxy]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.json :refer [wrap-json-params
                                          wrap-json-response]]
            [ring.util.response :as r]
            [compojure.core :refer :all]))

(defroutes app
  (GET "/v1/hello" {params :params}
    (let [name (get params :name "World")]
      (-> (r/response {:message (format "Hello, %s" name)})))))

(def handler (wrap-lambda-url-proxy
               (wrap-json-response
                 (wrap-json-params
                   (wrap-params
                     (wrap-keyword-params
                       app))))))

(deflambdafn example.LambdaFn [is os ctx]
  (with-open [writer (io/writer os)]
    (let [request (parse-stream (io/reader is :encoding "UTF-8") true)]
      (generate-stream (handler request) writer))))

```

It is a common to use AWS Scheduled Events to warmup the Lambda function.
For this case `ring-lambda-url-proxy` provides a configuration where
Scheduled Event can be mapped to regular Ring GET route like this:

```clojure
(wrap-lambda-url-proxy app {:scheduled-event-route "/warmup"})

(defroutes app
  (GET "/warmup" request {:status 200 :body "Scheduled event for warmup"}))

```

If you have not configured `:scheduled-event-route` and Lambda function is
called via Scheduled Event the error will be thrown.

## Lambda Function URL vs API Gateway

Lambda Function URLs provide a simpler alternative to API Gateway for HTTP endpoints. The main differences in request format:

**API Gateway Lambda Proxy:**
```clojure
{:httpMethod "GET"
 :path "/hello"
 :queryStringParameters {:name "world"}
 :headers {...}}
```

**Lambda Function URL:**
```clojure
{:requestContext {:http {:method "GET"}}
 :rawPath "/hello"
 :rawQueryString "name=world"
 :headers {...}}
```

This library handles the Lambda Function URL format automatically.

## Attribution

This work is heavily based on [mhjort/ring-apigw-lambda-proxy](https://github.com/mhjort/ring-apigw-lambda-proxy) by Markus Hjort. The core logic and design patterns are preserved, with modifications to handle Lambda Function URL request/response format instead of API Gateway Lambda proxy format.

## Design

This is a tiny library with zero dependencies. That is the reason why JSON
parsing of AWS Lambda Function URL request/response is not included.