(ns cdnce.cdn-exercise
  (:import
   [java.time Duration]
   [com.uber.cadence.client WorkflowClient]
   [com.uber.cadence.client WorkflowOptions$Builder]
   [cdnce.greet.workflow IGreet GreetImpl]))

(defn -main
  [& [args]]
  (let
   [^WorkflowClient i-workflowClient (WorkflowClient/newInstance "test-domain")
    i-workflow-options (->
                        (WorkflowOptions$Builder.)
                        (.setExecutionStartToCloseTimeout (Duration/ofSeconds 3600))
                        (.setTaskList "HelloWorldTaskList")
                        (.setWorkflowId args)
                        (.build))
    i-workflow (.newWorkflowStub i-workflowClient cdnce.greet.workflow.IGreet i-workflow-options)]
    (WorkflowClient/start 
     (reify com.uber.cadence.workflow.Functions$Func1
       (apply [_ t1]
         (.greet i-workflow t1))) 
     "Fred")
    
    (.updateGreeting i-workflow "Barney")
    (.updateGreeting i-workflow "Bye")))