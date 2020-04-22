(ns cdnce.greet.activities
  (:import
   [java.io PrintStream]))

(definterface IGreetActivities
  ;; Note: adding an annotation here like
  ;; ^{ActivityMethod true {:scheduleToCloseTimeoutSeconds 100}}
  ;; won't work as clojure casts 100 to a long, and cadence will complain
  ;; as it's expecting an int. You have to use builder when you're creating
  ;; the stub.
  ;; IGreetActivities::say
  (^void sayNow [^String message])
  ;; IGreetActivities::sayAfterDelay
  (^Boolean sayAfterDelay [^String message]))

;; deftype implementation
;; 

(deftype
 GreetActivitiesImpl
 [^java.io.PrintStream out]
  :load-ns false
  IGreetActivities
  (sayNow
    [this message]
    (.println out (str "[System/out] GreetActivitiesImpl says: " message)))
  (sayAfterDelay
    [this message]
    (.print out (str "[System/out] GreetActivitiesImpl is waiting. "))
    (Thread/sleep 5000)
    (.println out (str "[System/out] GreetActivitiesImpl then says: " message))
    true))

