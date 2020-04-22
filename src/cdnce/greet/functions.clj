(ns cdnce.greet.functions
  (:require
   [clojure.string :as str])
  (:import
   [com.uber.cadence.workflow Workflow]
   [com.uber.cadence.workflow Async]
   [com.uber.cadence.workflow Functions$Func1]))

;; utility
;; 
(defn ^java.util.function.Function as-supplier [f]
  (reify java.util.function.Supplier
    (get [this] (f))))

(defn ^java.util.function.Function as-function [f]
  (reify java.util.function.Function
    (apply [this arg] (f arg))))

(defn greet [logger greeting name]
  (let [ucase-er (Workflow/newChildWorkflowStub
               cdnce.ucase.workflow.IUcase)]
    (.info
     logger
     (.convertGreeting 
      ucase-er 
      (str greeting " " name)))))

(defn update-greeting! [logger state-atom greeting]
  (.info
   logger
   (str "setting greeting from "
        (pr-str (:greeting (deref state-atom))) " to "
        (pr-str greeting)))
  (swap!
   state-atom
   into
   {:greeting greeting}))

(defn wait-for-signal 
  [logger state-atom name]
  (.info
   logger
   (str "Starting 'wait-for-signal'."))
  (letfn
   [(something-to-do?
      []
      (let
       [Q (get (deref state-atom) :signal-queue)]
        (if (or (nil? Q) (empty? Q))
          false
          (let
           [{{previous-greeting :previous
              current-greeting :new} :updated-greeting} (peek Q)]
            (swap!
             state-atom
             update
             :signal-queue
             pop)
            (if (not= current-greeting previous-greeting)
              true
              false)))))]
    (while
     (and (< (:count (deref state-atom)) 10)
          (not= "Bye" (:greeting (deref state-atom))))
      
      (swap!
       state-atom
       update
       :count
       inc)
      
      (.info logger
             (str
              (:count (deref state-atom)) " : "
              (:greeting (deref state-atom)) " " name))
      
      (let 
       [current-greeting (str (:greeting (deref state-atom)) " " name)
        ucase-er (Workflow/newChildWorkflowStub
                  cdnce.ucase.workflow.IUcase)
        converted-greeting (.convertGreeting
                            ucase-er
                            current-greeting)]
        
      ;; run an activity (synchronous)
        (.sayNow
         (get @state-atom :activities)
         converted-greeting)

      ;; run an activity (asynchronous)
        (let
         [prom (Async/function
                (reify com.uber.cadence.workflow.Functions$Func1
                  (apply [_ t1]
                    (.sayAfterDelay (get @state-atom :activities) t1)))
                converted-greeting)
          result (.get prom)]
          (.info logger (str "Async promise result is " (pr-str result)))))
      
      (Workflow/await
       (as-supplier 
        something-to-do?)))
    
    (.sayNow
     (get @state-atom :activities)
     (str "Exiting with " (:greeting (deref state-atom))))))

(defn wait-for-signal-thread-local 
  [logger state-atom name]
  (.info
   logger
   (str "Starting 'wait-for-signal-thread-local'."))
  (let
   [local-greeting
    (proxy
     [ThreadLocal] []
      (initialValue []
        (get (deref state-atom) :greeting)))]
    (while
     (and (< (:count (deref state-atom)) 10)
          (not= "Bye" (:greeting (deref state-atom))))
      (swap!
       state-atom
       update
       :count
       inc)
      
      (.info logger
             (str
              (:count (deref state-atom)) " : "
              (:greeting (deref state-atom)) " " name))
      
      (let
       [current-greeting (str (:greeting (deref state-atom)) " " name)
        ucase-er (Workflow/newChildWorkflowStub
                  cdnce.ucase.workflow.IUcase)
        converted-greeting (.convertGreeting
                            ucase-er
                            current-greeting)]

      ;; run an activity (synchronous)
        (.sayNow
         (get @state-atom :activities)
         converted-greeting)

      ;; run an activity (asynchronous)
        (let
         [prom (Async/function
                (reify com.uber.cadence.workflow.Functions$Func1
                  (apply [_ t1]
                    (.sayAfterDelay (get @state-atom :activities) t1)))
                converted-greeting)
          result (.get prom)]
          (.info logger (str "Async promise result is " (pr-str result)))))
      
      (Workflow/await
       (as-supplier
        (fn []
          (not=
           (get (deref state-atom) :greeting)
           (.get local-greeting)))))
      (.set local-greeting (get (deref state-atom) :greeting)))
    
    (.sayNow
     (get @state-atom :activities)
     (str "Exiting with " (:greeting (deref state-atom))))))
