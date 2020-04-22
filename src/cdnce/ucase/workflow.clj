(ns cdnce.ucase.workflow
  (:require 
   [clojure.string :as str])
  (:import
   [java.time Duration]
   [com.uber.cadence.workflow Workflow WorkflowMethod SignalMethod QueryMethod]
   [com.uber.cadence.activity ActivityOptions$Builder]
   [org.slf4j Logger]))

(definterface IUcase
  ;; IUcase::convertGreeting
  (^{WorkflowMethod true}
   ^String convertGreeting [^String greeting]))

(defn co-init []
  [[] nil])

(defn co-post-init [this & [constr-args]])

(defn co-convertGreeting
  [this greeting]
  (str/upper-case greeting))

(gen-class
 :name "cdnce.ucase.workflow.UcaseImpl"
 :implements [cdnce.ucase.workflow.IUcase]
 :state "state"
 :init "init"
 :post-init "post-init"
 :constructors {[] []}
 :prefix "co-"
 :load-impl-ns true
 :main false)
