(ns cdnce.greet.workflow
  (:require
   [cdnce.greet.functions :as impl]
   [cdnce.greet.activities]
   [clojure.string :as str])
  (:import
   [java.time Duration]
   [com.uber.cadence.workflow Workflow WorkflowMethod SignalMethod QueryMethod]
   [com.uber.cadence.activity ActivityOptions$Builder]
   [org.slf4j Logger]))

(def logger (Workflow/getLogger "cdnce.greet.workflow"))

(defn state-watcher [watcher-key atom old-state new-state]
  (let
   [updated? (not= (:greeting old-state)
                   (:greeting new-state))]
    (when updated?
      (swap!
       atom
       (fn [a]
         (update
          a
          :signal-queue
          conj
          {:updated-greeting
           {:previous (:greeting old-state)
            :new (:greeting new-state)}}))))))

(defn co-init []
  [[] (atom
       {:greeting "Hello"
        :count 0
        :signal-queue clojure.lang.PersistentQueue/EMPTY
        :activities
        (Workflow/newActivityStub
         cdnce.greet.activities.IGreetActivities
         (->
          (ActivityOptions$Builder.)
          (.setScheduleToCloseTimeout (Duration/ofSeconds 1200))
          (.build)))})])

(defn co-post-init [this & [constr-args]]
  (add-watch
   (.state this)
   this
   state-watcher))

;; Actual Workflow Stuff
;; 

(definterface IGreet
  ;; IGreet::greet
  (^{WorkflowMethod true}
   ^void greet [name])
  ;; IGreet::updateGreeting
  (^{SignalMethod true}
   ^void updateGreeting [greeting])
  ;; IGreet::getCount
  (^{QueryMethod true}
   ^int getCount []))

(defn co-greet
  [this name]
  (if
   (str/starts-with? name "_")
    (do
      (remove-watch (.state this) this)
      (impl/wait-for-signal-thread-local
       logger
       (.state this)
       name))
    (impl/wait-for-signal
     logger
     (.state this)
     name)))

(defn co-updateGreeting
  [this greeting]
  (impl/update-greeting!
   logger
   (.state this)
   greeting))

(defn co-getCount
  [this]
  (get @(.state this) :count))

(gen-class
 :name "cdnce.greet.workflow.GreetImpl"
 :implements [cdnce.greet.workflow.IGreet]
 :state "state"
 :init "init"
 :post-init "post-init"
 :constructors {[] []}
 :prefix "co-"
 :load-impl-ns true
 :main false)
