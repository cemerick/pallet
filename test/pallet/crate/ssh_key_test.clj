(ns pallet.crate.ssh-key-test
  (:use [pallet.crate.ssh-key] :reload-all)
  (:require [pallet.template :as template]
            [pallet.resource :as resource]
            [pallet.stevedore :as stevedore]
            [pallet.utils :as utils]
            [pallet.resource.directory :as directory]
            [pallet.resource.file :as file]
            [pallet.resource.remote-file :as remote-file])
  (:use clojure.test
        pallet.test-utils))

(use-fixtures :each with-null-target)

(deftest authorized-keys-template-test
  (is (= "file=$(getent passwd userx | cut -d: -f6)/.ssh/authorized_keys
cat > ${file} <<EOF\nkey1\nkey2\nEOF
chmod 0644 ${file}
chown userx ${file}
"
         (pallet.template/apply-templates authorized-keys-template ["userx" ["key1" "key2"]] ))))

(with-private-vars [pallet.crate.ssh-key
                    [authorize-key*]]
  (deftest authorize-key*-test
    (is (= (stevedore/do-script
            (directory/directory*
             "$(getent passwd userx | cut -d: -f6)/.ssh/"
             :owner "userx" :mode "755")
            (remote-file/remote-file*
             "$(getent passwd userx | cut -d: -f6)/.ssh/authorized_keys"
             :content "key1\nkey2"
             :owner "userx" :mode "0644"))
           (authorize-key* "userx" ["key1" "key2"] )))))

(deftest authorize-key-test
  (is (= (stevedore/do-script
          (directory/directory*
           "$(getent passwd user2 | cut -d: -f6)/.ssh/"
           :owner "user2" :mode "755")
          (remote-file/remote-file*
           "$(getent passwd user2 | cut -d: -f6)/.ssh/authorized_keys"
           :content "key3"
           :owner "user2" :mode "0644")
          (directory/directory*
           "$(getent passwd user | cut -d: -f6)/.ssh/"
           :owner "user" :mode "755")
          (remote-file/remote-file*
           "$(getent passwd user | cut -d: -f6)/.ssh/authorized_keys"
           :content "key1\nkey2"
           :owner "user" :mode "0644"))
         (pallet.resource/build-resources []
          (authorize-key "user" "key1")
          (authorize-key "user" "key2")
          (authorize-key "user2" "key3")))))

(deftest install-key*-test
  (is (= (str
          (directory/directory*
           "$(getent passwd fred | cut -d: -f6)/.ssh/"
           :owner "fred" :mode "755")
          (remote-file/remote-file*
           "$(getent passwd fred | cut -d: -f6)/.ssh/id"
           :content "private"
           :owner "fred" :mode "600")
          (remote-file/remote-file*
           "$(getent passwd fred | cut -d: -f6)/.ssh/id.pub"
           :content "public"
           :owner "fred" :mode "644"))
         (install-key* "fred" "id" "private" "public"))))

(deftest install-key-test
  (is (= (str
          (directory/directory*
           "$(getent passwd fred | cut -d: -f6)/.ssh/"
           :owner "fred" :mode "755")
          (remote-file/remote-file*
           "$(getent passwd fred | cut -d: -f6)/.ssh/id"
           :content "private"
           :owner "fred" :mode "600")
          (remote-file/remote-file*
           "$(getent passwd fred | cut -d: -f6)/.ssh/id.pub"
           :content "public"
           :owner "fred" :mode "644"))
         (pallet.resource/build-resources
          []
          (install-key "fred" "id" "private" "public")))))

(deftest generate-key*-test
  (is (= (stevedore/do-script
          (directory/directory*
           "$(getent passwd fred | cut -d: -f6)/.ssh/"
           :owner "fred" :mode "755")
          (stevedore/checked-script "ssh-keygen"
           (var key_path "$(getent passwd fred | cut -d: -f6)/.ssh/id_rsa")
           (if-not (file-exists? @key_path)
             (ssh-keygen -f @key_path -t rsa -N "\"\"")))
          (file/file*
           (stevedore/script @key_path)
           :owner "fred" :mode "0600")
          (file/file*
           (str (stevedore/script @key_path) ".pub")
           :owner "fred" :mode "0644"))
         (generate-key* "fred")))

  (is (= (stevedore/do-script
          (directory/directory*
           "$(getent passwd fred | cut -d: -f6)/.ssh/"
           :owner "fred" :mode "755")
          (stevedore/checked-script "ssh-keygen"
           (var key_path "$(getent passwd fred | cut -d: -f6)/.ssh/id_dsa")
           (if-not (file-exists? @key_path)
             (ssh-keygen -f @key_path -t dsa -N "\"\"")))
          (file/file*
           (stevedore/script @key_path)
           :owner "fred" :mode "0600")
          (file/file*
           (str (stevedore/script @key_path) ".pub")
           :owner "fred" :mode "0644"))
         (generate-key* "fred" :type "dsa")))

  (is (= (stevedore/do-script
          (directory/directory*
           "$(getent passwd fred | cut -d: -f6)/.ssh/"
           :owner "fred" :mode "755")
          (stevedore/checked-script "ssh-keygen"
           (var key_path "$(getent passwd fred | cut -d: -f6)/.ssh/identity")
           (if-not (file-exists? @key_path)
             (ssh-keygen -f @key_path -t rsa1 -N "\"\"")))
          (file/file*
           (stevedore/script @key_path)
           :owner "fred" :mode "0600")
          (file/file*
           (str (stevedore/script @key_path) ".pub")
           :owner "fred" :mode "0644"))
         (generate-key* "fred" :type "rsa1"))))

(deftest authorize-key-for-localhost*-test
  (is (= (stevedore/do-script
          (stevedore/script
           (var key_file "$(getent passwd fred | cut -d: -f6)/.ssh/id_dsa.pub")
           (var auth_file "$(getent passwd fred | cut -d: -f6)/.ssh/authorized_keys"))
          (directory/directory*
           "$(getent passwd fred | cut -d: -f6)/.ssh/"
           :owner "fred" :mode "755")
          (file/file*
           "$(getent passwd fred | cut -d: -f6)/.ssh/authorized_keys"
           :owner "fred" :mode "644")
          (stevedore/checked-script "authorize-key"
                                    (if-not (grep @(cat @key_file) @auth_file)
                                      (cat @key_file ">>" @auth_file))))
         (authorize-key-for-localhost* "fred" "id_dsa.pub")))

  (is (= (stevedore/do-script
          (stevedore/script
           (var key_file "$(getent passwd fred | cut -d: -f6)/.ssh/id_dsa.pub")
           (var auth_file "$(getent passwd tom | cut -d: -f6)/.ssh/authorized_keys"))
          (directory/directory*
           "$(getent passwd tom | cut -d: -f6)/.ssh/"
           :owner "tom" :mode "755")
          (file/file*
           "$(getent passwd tom | cut -d: -f6)/.ssh/authorized_keys"
           :owner "tom" :mode "644")
          (stevedore/checked-script "authorize-key"
                                    (if-not (grep @(cat @key_file) @auth_file)
                                      (cat @key_file ">>" @auth_file))))
         (authorize-key-for-localhost* "fred" "id_dsa.pub" :authorize-for-user "tom"))))

