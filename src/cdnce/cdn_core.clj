(ns cdnce.cdn-core
  (:import 
   [com.uber.cadence.worker Worker Worker$Factory]
   [cdnce.hello.workflow HelloImpl]
   #_[cdnce.greeting.workflow GreetingImpl]
   [cdnce.greet.workflow GreetImpl]
   [cdnce.ucase.workflow UcaseImpl]))

(defn -main
  [& args]
  (let
   [^Worker$Factory factory (Worker$Factory. "test-domain")
    ^Worker worker (.newWorker factory "HelloWorldTaskList")]
    (.registerWorkflowImplementationTypes 
     worker 
     ;;^"[Ljava.lang.Class;"
     (into-array
        [cdnce.hello.workflow.HelloImpl 
         ;cdnce.greeting.workflow.GreetingImpl
         cdnce.greet.workflow.GreetImpl
         cdnce.ucase.workflow.UcaseImpl]))
    (.start factory)))

