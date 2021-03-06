(ns pallet.crate.etc-default-test
 (:use
  clojure.test
  pallet.test-utils)
 (:require
  [pallet.crate.etc-default :as default]
  [pallet.resource :as resource]
  [pallet.stevedore :as stevedore]
  [pallet.resource.remote-file :as remote-file]
  [pallet.target :as target]))

(deftest test-tomcat-defaults
  (is (= (stevedore/do-script
          (stevedore/script
           (if (not (file-exists? "/etc/default/tomcat6.orig"))
             (cp "/etc/default/tomcat6" "/etc/default/tomcat6.orig")))
	  (remote-file/remote-file*
	   "/etc/default/tomcat6"
	   :owner "root:root"
	   :mode 644
	   :content "JAVA_OPTS=\"-Djava.awt.headless=true -Xmx1024m\"\nJSP_COMPILER=\"javac\""))
	 (test-resource-build
	  [nil {:image [:ubuntu]}]
	  (default/write "tomcat6"
	    :JAVA_OPTS "-Djava.awt.headless=true -Xmx1024m"
	    "JSP_COMPILER" "javac")))))

(deftest test-quoting
  (is (= "\"\\\"quoted value\\\"\""
        (#'default/quoted "\"quoted value\""))))
