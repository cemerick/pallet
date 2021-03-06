(ns pallet.task.providers
  "Provide information on the supported and enabled providers."
  (:require
   [pallet.utils :as utils]))

(defn- provider-properties []
  (apply
   hash-map
   (apply concat
          (filter #(re-find #"(.*)\.contextbuilder" (first %))
                  (utils/resource-properties "compute.properties")))))

(defn- enabled?
  [provider]
  (try
   (Class/forName provider)
   (catch java.lang.ClassNotFoundException e)))

(defn providers
  "Provide information on the supported and enabled providers."
  {:no-service-required true}
  [& _]
  (println "Pallet uses jcloud's providers.\n")
  (doseq [supported (provider-properties)
          :let [key (first supported)
                name (.substring key 0 (.indexOf key "."))]]
    (println
     (format
      "\t%15s\t %s"
      name
      (if (enabled? (second supported)) "Enabled" "Disabled"))))
  (println "\nProviders can be enabled by copying the corresponding jclouds jar")
  (println "into pallet/lib."))
