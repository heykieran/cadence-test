(ns cdnce.hello.functions
  (:require 
   [clojure.string :as str]))

(defn say-something [logger greeting name & [in-caps]]
  (.info
   logger
   (let
    [base-message (str "[cdnce.hello.functions] " greeting " " name)]
     (if in-caps
       (str/upper-case base-message)
       base-message))))
