(ns burp.demo
  (:require [burp.api :as api :refer [deftemplate]]))

;; load and define three templates from `resources`
(deftemplate test1 "test1.haml")
(deftemplate test2 "test2.haml")
(deftemplate test3 "test3.haml")

;; templates are functions from a single value to a string

;; the symbol % is bound to the value of the argument passed when
;; invoking a template function

(test1 [1 2 3 4])
;; => "<h1>Inline block<p>1</p><p>2</p><p>3</p><p>4</p></h1>"


(test2 {:a 1 :b 2 :c 3})
;; => "<h1>Destructuring & inline code<p>Key :a has value 1</p><p>Key :c has value 3</p><p>Key :b has value 2</p></h1>"


(test3 {:some-key 42})
;; => "<h1>Control flow<p>Nope.</p></h1>"


(test3 {:some-key :blahonga})
;; => "<h1>Control flow<p>Yep!</p></h1>"






