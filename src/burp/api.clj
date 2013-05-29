(ns burp.api
  (:require [burp.hiccup]
            [clojure.java.io :as io]))

(defn- resource-template
  [path]
  (let [source (io/reader (io/resource path))]
    (burp.hiccup/compile source)))

(defmacro deftemplate
  [name path]
  `(def ~name ~(resource-template path)))
