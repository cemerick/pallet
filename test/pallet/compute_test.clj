(ns pallet.compute-test
  (:use [pallet.compute] :reload-all)
  (require
   [pallet.utils]
   [org.jclouds.compute :as jclouds])
  (:use clojure.test
        pallet.test-utils)
  (:import [org.jclouds.compute.domain NodeState OsFamily]))

(deftest compute-node?-test
  (is (not (compute-node? 1)))
  (is (compute-node? (make-node "a")))
  (is (every? compute-node? [(make-node "a") (make-node "b")])))

(deftest node-counts-by-tag-test
  (is (= {:a 2}
         (node-counts-by-tag [(make-node "a") (make-node "a")]))))

(deftest running?-test
  (is (not (jclouds/running? (make-node "a" :state NodeState/TERMINATED))))
  (is (jclouds/running? (make-node "a" :state NodeState/RUNNING))))

(deftest print-method-test
  (is (= "             a\t  null\n\t\t null\n\t\t RUNNING\n\t\t public:   private: " (with-out-str (print (make-node "a"))))))

(deftest node-os-family-test
  (is (= :ubuntu
         (node-os-family
          (make-node
           "t"
           :image (make-image "1" :os-family OsFamily/UBUNTU))))))
