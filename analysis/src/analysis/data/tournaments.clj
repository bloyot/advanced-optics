(ns analysis.data.tournaments
  (:require
    [analysis.db :as db]
    [clj-http.client :as http]
    [clojure.java.io :as io]
    [clojure.set :refer [rename-keys]]
    [clojure.string :as string]
    [cheshire.core :refer [parse-string]]
    [analysis.util :as util]))

(def base-url "https://listfortress.com/api/v1/tournaments")
(def data-dir (util/make-path "resources" "data" "listfortress"))
(def tournament-file (util/make-path data-dir "tournaments.json"))
(def tournaments-dir (util/make-path data-dir "tournaments"))

;; listfortress api calls
(defn fetch-all-tournaments!
  "Grab tournaments manifest from listfortress api and store them in the data dir"
  []
  (let [response (http/get base-url {:accept :json})]
    (if (= 200 (:status response))
      (spit tournament-file (:body response))
      (println (str "failed to fetch tournament manifest response code=" (:status response))))))

(defn fetch-tournament-details!
  "Grab a tournament from the listfortress api for the given tournament id and store it in the data dir"
  [id]
  (println (str "fetching lists for tournament " id))
  (let [target-file (util/make-path tournaments-dir (str id ".json"))]
    (if (not (util/file-exists? target-file))
      (let [response (http/get (str base-url "/" id) {:accept :json})]
        (if (= 200 (:status response))
          (spit target-file (:body response))
          (println (str "failed to fetch tournament " id ", response code=" (:status response)))))
      (println (str "Skipping fetch of lists for tournament " id ", the file already exists")))))

;; db loading
(defn load-tournaments!
  "Load tournaments from file into the database."
  []
  (db/clear-tournament-data!)
  (let [tournaments (-> tournament-file
                        slurp
                        (parse-string true))]
    (->> tournaments
        (map #(select-keys % [:date :country :format_id :tournament_type_id :id]))
        (map #(rename-keys % {:tournament_type_id :type
                              :format_id :format
                              :id :tournament_id}))
        db/insert-tournaments!)))

(defn load-tournament-lists!
  "Load the lists for a tournament into the database from a stored file"
  [id]
  (let [tournament (-> (util/make-path tournaments-dir (str id ".json"))
                       slurp
                       (parse-string true))
        participants (:participants tournament)
        make-list (fn [p]
                    {:faction (:faction (parse-string (:list_json p) true))
                     :list_json (:list_json p)
                     :swiss_rank (:swiss_rank p)
                     :top_cut_rank (:top_cut_rank p)})]
    (db/update-participant-count! id (count participants))
    (db/insert-lists! id (map make-list participants))))

(defn load-all-lists!
  "Clear and load all lists we currently have saved in the data dir into the database"
  []
  (db/clear-list-data!)
  (let [files-to-load (->> (io/file tournaments-dir)
                          file-seq
                           (map #(.getName %))
                          (filter #(string/ends-with? % ".json"))
                          (map #(string/split % #".json"))
                          flatten
                          (map #(Integer/parseInt %)))]
    (map load-tournament-lists! files-to-load)))
