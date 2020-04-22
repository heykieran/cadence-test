(ns cdnce.cdn-activity-runner
  (:import
   [com.uber.cadence.worker Worker Worker$Factory]
   [cdnce.greet.activities GreetActivitiesImpl]))

(defn -main
  [& args]
  (let
   [^Worker$Factory factory (Worker$Factory. "test-domain")
    ^Worker worker (.newWorker factory "HelloWorldTaskList")]
    (.registerActivitiesImplementations
     worker
     ;;^"[Ljava.lang.Object;"
     (into-array Object
      [(new GreetActivitiesImpl System/out)]))
    (.start factory)))

(-main)