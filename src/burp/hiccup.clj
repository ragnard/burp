(ns burp.hiccup
  (:refer-clojure :exclude [compile])
  (:require [burp.parser   :as parser]
            [hiccup.core    :as hiccup]
            [clojure.string :as string])
  (:import [burp.parser
            CodeBlock
            Comment
            Document
            Doctype
            Element
            Text
            FilteredBlock
            XmlProlog
            InlineCode]))


(def vec-conj (fnil conj []))

(defn flat-conj
  [vec xs]
  (apply vec-conj vec xs))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Hiccup Translation

(defprotocol ToHiccup
  (->hiccup [this]))

(defn element-tag
  [{:keys [name id classes]}]
  (keyword (str (or name "div")
                (when id
                  (str "#" id))
                (when-not (empty? classes)
                  (str "." (string/join "." classes))))))

(defn element->hiccup
  [{:keys [attributes inline-content children] :as element}]
  (cond-> [(element-tag element)]

          (not (empty? attributes))
          (conj attributes)

          (not (string/blank? inline-content)) 
          (conj inline-content)

          children
          (flat-conj (mapv ->hiccup children))))

(defn code-block->hiccup
  [{:keys [code children]}]
  (let [data (read-string (str "[" code "]"))]
    (reverse (into '() (flat-conj data (mapv ->hiccup children))))))

(defn inline-code->hiccup
  [{:keys [code]}]
  (read-string (str "(str " code ")")))

(defn comment->hiccup
  [{:keys [text condition children]}]
  (concat (if condition
            ["<!--[" condition "]>"]
            ["<!-- "])
          (when text
            [text])
          (mapv ->hiccup children)
          (if condition
            ["<![endif]-->"]
            [" -->"])))

(defmulti filtered-block->hiccup :type)

(defmethod filtered-block->hiccup :plain
  [filtered-block]
  (apply str (interpose "\n" (:lines filtered-block))))

(defmethod filtered-block->hiccup :javascript
  [filtered-block]
  [:script (apply str (interpose "\n" (:lines filtered-block)))])

(defmethod filtered-block->hiccup :css
  [filtered-block]
  [:style (apply str (interpose "\n" (:lines filtered-block)))])

(defmethod filtered-block->hiccup :cdata
  [filtered-block]
  (str "<![CDATA["
       (apply str (interpose "\n" (:lines filtered-block)))
       "]]>"))

(defmethod filtered-block->hiccup :default
  [filtered-block]
  (throw (ex-info (format "Unsupported filter type: %s" (:type filtered-block))
                  {:node filtered-block})))

(comment (defn- doctype->hiccup
           [{:keys [name]}]
           (str "<!DOCTYPE "
                (apply str (header/lookup-doctype name))
                ">\n")))

(comment (defn- xml-prolog->hiccup
           [{:keys [opts]}]
           (header/xml-prolog opts)))

(extend-protocol ToHiccup
  Element
  (->hiccup [this] (element->hiccup this))

  Text
  (->hiccup [this] (:text this))

  FilteredBlock
  (->hiccup [this] (filtered-block->hiccup this))
  
  Comment
  (->hiccup [this] (comment->hiccup this))
  
  Doctype
  (->hiccup [this]
    (throw (ex-info "not implemented" {}))
    (comment  (doctype->hiccup this)))

  XmlProlog
  (->hiccup [this]
    (throw (ex-info "not implemented" {}))
    (comment (xml-prolog->hiccup this)))
  
  Document
  (->hiccup [this] (concat (mapv ->hiccup (:header this))
                           (mapv ->hiccup (:elements this))))

  CodeBlock
  (->hiccup [code-block]
    (code-block->hiccup code-block))

  InlineCode
  (->hiccup [inline-code]
    (inline-code->hiccup inline-code)))

(defn- to-hiccup
  [parse-tree]
  (->hiccup parse-tree))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Public API



(defn hiccup
  "Returns hiccup data from the given haml-source. A haml-source is
  anything that satisfies the CharSeq protocol, typically a String or
  a Reader."
  [haml-source]
  (let [parse-tree (parser/parse-tree haml-source)]
    (to-hiccup parse-tree)))

(defn compile
  [template-source]
  (let [hiccup-data (seq (hiccup template-source))]
   (eval `(fn [data#] (let [~'% data#] (hiccup/html ~@hiccup-data))))) )








