(ns cdnce.hello.workflow
  (:require
   [cdnce.hello.functions :as impl])
  (:import
   [com.uber.cadence.workflow Workflow WorkflowMethod SignalMethod QueryMethod]
   [com.uber.cadence.activity ActivityOptions$Builder]
   [org.slf4j Logger]))

(def logger (Workflow/getLogger "cdnce.hello"))

(defn co-init []
  [[] (atom 
       {:greeting "Hello"})])

(definterface IHello
  ;; IHello::sayHello
  (^{WorkflowMethod true}
   sayHello [name]))

(defn co-sayHello
  [this name]
  (impl/say-something logger (get @(.state this) :greeting) name))

(gen-class
 :name "cdnce.hello.workflow.HelloImpl"
 :implements [cdnce.hello.workflow.IHello]
 :state "state"
 :init "init"
 :constructors {[] []}
 :prefix "co-"
 :load-impl-ns true
 :main false)
