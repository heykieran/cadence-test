(ns cdnce.greeting.workflow
  (:import
   [java.time Duration]
   [com.uber.cadence.workflow Workflow WorkflowMethod SignalMethod QueryMethod]
   [com.uber.cadence.activity ActivityOptions$Builder]
   [org.slf4j Logger]))

(definterface IGreeting
  ;; IGreeting::composeGreeting
  (^{WorkflowMethod true}
   ^String composeGreeting [^String greeting ^String name]))

(defn cw-init []
  [[] nil])

(defn cw-post-init [this & [constr-args]])

(defn cw-composeGreeting
  [this greeting name]
  (str "Well " greeting " there, " name "!"))

(gen-class
 :name "cdnce.greeting.workflow.GreetingImpl"
 :implements [cdnce.greeting.workflow.IGreeting]
 :state "state"
 :init "init"
 :post-init "post-init"
 :constructors {[] []}
 :prefix "cw-"
 :load-impl-ns true
 :main false)
