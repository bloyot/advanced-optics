(ns analysis.util
  (:import (java.io File)))

(def SEP (File/separator))

(defn make-path
  [& paths]
  (clojure.string/join SEP paths))

(defn file-exists?
  [path]
  (.exists (clojure.java.io/file path)))