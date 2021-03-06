(ns pallet.resource.remote-directory-test
  (:use [pallet.resource.remote-directory] :reload-all)
  (:use clojure.test
        pallet.test-utils)
  (:require
   [pallet.stevedore :as stevedore]
   [pallet.resource.directory :as directory]
   [pallet.resource.remote-file :as remote-file]
   [pallet.utils :as utils]))

(deftest remote-directory-test
  (is (= (stevedore/checked-commands
          "remote-directory"
          (directory/directory* "/path" :owner "fred")
          (remote-file/remote-file*
           "${TMPDIR-/tmp}/file.tgz" :url "http://site.com/a/file.tgz" :md5 nil)
          (stevedore/script
           (cd "/path")
           (tar xz "--strip-components=1" -f "${TMPDIR-/tmp}/file.tgz")))
         (test-resource-build
          [nil {}]
          (remote-directory
           "/path"
           :url "http://site.com/a/file.tgz"
           :unpack :tar
           :owner "fred")))))
