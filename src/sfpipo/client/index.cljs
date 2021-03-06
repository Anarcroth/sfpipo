(ns sfpipo.client.index
  (:require [reagent.core :as r :refer [atom]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<!]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(enable-console-print!)

(defonce pong-res (r/atom ""))
(defonce file-upload-res (r/atom ""))
(defonce file-name-input (r/atom ""))

(defn- call-ping
  []
  (go (let [response (<! (http/get "/ping"))]
        (= (:status response) 200))))

(defn- handle-ping-press []
  (if (call-ping)
    #(swap! pong-res (fn [] (str "Pong!")))
    #(swap! pong-res (fn [] (str "Service down!")))))

(defn ping-button []
  [:div {:id "ping-button"}
   [:input {:type "button" :value "Ping" :class ["button"]
            :on-click (handle-ping-press)}]])

(defn- get-file [file-id]
  (go (let [response (<! (http/get (str "/file/" file-id)))]
        (prn response))))

(defn download-button []
  [:div {:id "download-button"}
   [:form
    [:input {:type "text"}]
    [:input {:type "submit" :value "Download file"}]]])

(defn- save-file-to-db [f]
  (go (let [response (<! (http/post "/file/upload" {:multipart-params [["file" f]]}))]
        (= (:status response) 200))))

(defn- handle-file-upload [f]
  (if (save-file-to-db f)
    #(swap! file-upload-res (fn [] (str "File uploaded!")))
    #(swap! file-upload-res (fn [] (str "File upload failed!")))))

(defn file-input []
  [:input {:type "file"
           :on-change
           (fn [this]
             (if (not (= "" (-> this .-target .-value)))
               (let [^js/File file (-> this .-target .-files (aget 0))]
                 (handle-file-upload file)
                 (set! (-> this .-target .-value) ""))))}])

(defn- parse-file-res [res]
  (as-> res r
    (:body r)
    (.parse js/JSON r)
    (js->clj r :keywordize-keys true)))

(defn- delete-file [file-id]
  (go (let [response (<! (http/delete (str "/file/" file-id)))]))
  (create-file-table))

(defn- create-file-table [f]
  (r/render-component
   [:div
    [:p]
    [:table>tbody
     [:tr
      [:th "Id"]
      [:th "Name"]
      [:th "Size"]
      [:th "Action"]]
     [:tr
      [:td (:id f)]
      [:td (:name f)]
      [:td (:size f)]
      [:td [:button {:on-click #(delete-file (:id f))} "Delete"]]
      ]]]
   (.getElementById js/document "file-table")))

(defn get-file-from-db
  "Get file from db and output file name, size, link, etc"
  [file-id]
  (go (let [response (<! (http/get (str "/file/" file-id)))]
        (if (= (:status response) 200)
          (create-file-table (parse-file-res response))))))

(defn file-search-form []
  [:div
   [:input {:type "text"
            :value @file-name-input
            :on-change #(reset! file-name-input (.-value (.-target %)))
            }]
   [:button  {:on-click #(get-file-from-db @file-name-input)} "Search for file"]])

(defn get-lists
  "Get the default 3 functionalities for the current time being."
  []
  [:div
   [:p @pong-res]
   [ping-button]
   [:p @file-upload-res]
   [file-input]
   [file-search-form]])

(defn init-lists []
  (r/render-component
   [get-lists]
   (.getElementById js/document "sfpipo-main")))
