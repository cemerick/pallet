(ns pallet.template
  "Template file writing"
  (:require
   pallet.compat
   [pallet.strint :as strint]
   [pallet.target :as target]
   [pallet.utils :as utils])
  (:use [pallet.stevedore :only [script]]
        [pallet.resource.file]
        [clojure.contrib.logging]))

(pallet.compat/require-contrib)

(defn get-resource
  "Loads a resource. Returns a URI."
  [path]
  (-> (clojure.lang.RT/baseLoader) (.getResource path)))

(defn path-components
  "Split a path into directory, basename and extension components"
  [path]
  (let [f (java.io.File. path)
        filename (.getName f)
        i (.lastIndexOf filename "." )]
    [(.getParent f)
     (if (neg? i) filename (.substring filename 0 i))
     (if (neg? i) nil (.substring filename (inc i)))]))

(defn pathname
  "Build a pathname from a list of path and filename parts.  Last part is assumed
   to be a file extension."
  [& parts]
  (let [ext (last parts)]
    (str (apply str (interpose java.io.File/separator (butlast parts)))
         (if ext (str "." ext)))))

(defn candidate-templates
  "Generate a prioritised list of possible template paths."
  [path tag template]
  (let [[dirpath base ext] (path-components path)
        variants (fn [specifier]
                   (let [p (pathname
                            dirpath
                            (if specifier (str base "_" specifier) base)
                            ext)]
                     [p (str "resources/" p)]))]
    (concat
     (variants tag)
     (variants (name (target/os-family template)))
     (variants (name (target/packager template)))
     (variants nil))))

(defn find-template
  "Find a template for the specified path, for application to the given node.
   Templates may be specialised."
  [path node-type]
  {:pre [node-type]}
  (some
   get-resource
   (candidate-templates path (node-type :tag) (node-type :image))))

(defn interpolate-template
  "Interpolate the given template."
  [path values]
  (strint/<<!
   (utils/load-resource-url
    (find-template path (target/node-type)))
   (utils/map-with-keys-as-symbols values)))

;;; programatic templates - umm not really templates at all

(defmacro deftemplate [template [& args] m]
  `(defn ~template [~@args]
     ~m))

(defn- apply-template-file [[file-spec content]]
  (trace (str "apply-template-file " file-spec \newline content))
  (let [path (:path file-spec)]
    (string/join ""
                 (filter (complement nil?)
                         [(script (var file ~path) (cat > @file <<EOF))
                          content
                          "\nEOF\n"
                          (when-let [mode (:mode file-spec)]
                            (script (do ("chmod" ~mode @file))))
                          (when-let [group (:group file-spec)]
                            (script (do ("chgrp" ~group @file))))
                          (when-let [owner (:owner file-spec)]
                            (script (do ("chown" ~owner @file))))]))))

;; TODO - add chmod, owner, group
(defn apply-templates [template-fn args]
  (string/join "" (map apply-template-file (apply template-fn args))))
