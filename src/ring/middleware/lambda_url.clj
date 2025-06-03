(ns ring.middleware.lambda-url
  (:require [clojure.string :as string])
  (:import [java.io ByteArrayInputStream]))

(defn- parse-query-string [query-string]
  (when (and query-string (not (string/blank? query-string)))
    query-string))

(defn- request->http-method [request]
  (-> (get-in request [:requestContext :http :method])
      (string/lower-case)
      (keyword)))

(defn- keyword->lowercase-string [k]
  (string/lower-case (name k)))

(defn- map-keys [f m]
  (into {} (map (fn [[k v]] [(f k) v]) m)))

(defn- lambda-url-request->ring-request [lambda-url-request]
  {:pre [(every? #(get-in lambda-url-request %) [[:requestContext :http :method] [:rawPath]])
         (contains? #{"GET" "POST" "OPTIONS" "DELETE" "PUT" "PATCH"} (get-in lambda-url-request [:requestContext :http :method]))]}
  {:uri (:rawPath lambda-url-request)
   :query-string (parse-query-string (:rawQueryString lambda-url-request))
   :request-method (request->http-method lambda-url-request)
   :headers (map-keys keyword->lowercase-string (:headers lambda-url-request))
   :body (when-let [body (:body lambda-url-request)] (ByteArrayInputStream. (.getBytes body "UTF-8")))})

(defn- no-scheduled-route-configured-error [request]
  (throw (ex-info "Got Scheduled Event but no scheduled-event-route configured"
                  {:request request})))

(defn- lambda-url->ring-request [request scheduled-event-route]
  (let [scheduled-event? (= "Scheduled Event" (:detail-type request))]
    (cond
      (and scheduled-event? scheduled-event-route) (lambda-url-request->ring-request {:rawPath scheduled-event-route
                                                                                       :rawQueryString ""
                                                                                       :headers nil
                                                                                       :requestContext {:http {:method "GET"}}})
      scheduled-event? (no-scheduled-route-configured-error request)
      :else (lambda-url-request->ring-request request))))

(defn wrap-lambda-url-proxy
  ([handler] (wrap-lambda-url-proxy handler {}))
  ([handler {:keys [scheduled-event-route]}]
   (fn [request]
     (let [response (handler (lambda-url->ring-request request scheduled-event-route))]
       {:statusCode (:status response)
        :headers (:headers response)
        :body (:body response)}))))
