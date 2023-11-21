;; manage loading and parsing the xwing data from it's raw source
(ns analysis.data.ships
  (:require [analysis.db :as db]
            [analysis.util :as util]
            [cheshire.core :refer :all]))

(def data-path (util/make-path "resources" "data" "xwingdata" "pilots"))
(def data-dir (clojure.java.io/file data-path))

;; helpers to parse stats which can sometimes have odd formats
(defn parse-stat
  [stat]
  {(keyword (:type stat)) (or (:value stat))})


(defn load-ship-data!
  "Load raw xwing ship data for a single ship file.
  ship-file is the java.io.file"
  [ship-file]
  (let [raw-ship (-> ship-file
                     slurp
                     (parse-string true))
        faction (:faction raw-ship)
        ship-name (:xws raw-ship)
        pilots (:pilots raw-ship)
        stats (->> (:stats raw-ship)
                   (map parse-stat)
                   ;; ensure attack (or any duplicated value is mapped as a vector of values
                   (apply (partial merge-with (comp vec flatten conj list))))
        partial-ship (merge {:faction faction
                             :ship ship-name}
                            stats)]
    (println (str "loading " ship-file "..."))
    (for [{:keys [force xws initiative]} pilots]
      (db/insert-ship! (merge partial-ship {:force (or (:value force) 0) :pilot xws :initiative initiative})))))


(defn load-all-ship-data!
  "Load the raw xwing ship data into the db"
  []
  (db/clear-ship-data!)
  (let [files (->> data-dir
                   file-seq
                   (filter #(clojure.string/ends-with? (.getPath %) ".json")))]
    (->> files
         (map load-ship-data!)
         flatten
         (reduce +))))