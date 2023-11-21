;; namespace for database connections and queries
(ns analysis.db
  (:require
    [analysis.constants :as constants]
    [honey.sql :as sql]
    [honey.sql.helpers :refer [columns delete-from from insert-into join select
                               values where] :as h]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def db-spec
  {:dbtype "sqlite"
   :dbname "db/analysis.db"})

(def ds (jdbc/get-datasource db-spec))

(defn get-update-count
  [result]
  (:next.jdbc/update-count (first result)))

(defn resolve-pilot-name
  "Handle inconsistencies in pilot naming"
  [faction ship pilot-name]
  (or (get-in constants/pilot-alternate-name-mappings
              [(keyword faction) (keyword ship) (keyword pilot-name)]) pilot-name))

;; selects
(defn get-ship-by-id
  [faction ship pilot]
  (let [rs (jdbc/execute! ds (-> (select :faction :pilot :ship :agility :initiative :hull :shields :force [:value :attack])
                                 (from :ship)
                                 (join :ship_attack [:= :ship.ship_id :ship_attack.ship_id])
                                 (where [:= :faction faction]
                                        [:= :ship ship]
                                        [:= :pilot (resolve-pilot-name faction ship pilot)])
                                 sql/format)
                          {:builder-fn rs/as-unqualified-maps})
        attack-vals (vec (map :attack rs))]
    (assoc (first rs) :attack attack-vals)))

(def get-ship-by-id-memo (memoize get-ship-by-id))

(defn get-lists
  "Select all lists with the corresponding faction(s) within the given date range"
  [factions start end & min-players]
  (jdbc/execute! ds (-> (select :list_xws)
                        (from :list)
                        (join :tournament [:= :tournament.tournament_id :list.tournament_id])
                        (where
                          [:is-not :list_xws nil]
                          [:in :faction factions]
                          [:> :num_players (or min-players 0)]
                          [:>= :date (str start)]
                          [:<= :date (str end)])
                        sql/format)
                 {:builder-fn rs/as-unqualified-maps}))

(defn ship-count
  "Queries for the total number of ships loaded"
  []
  (let [result (jdbc/execute! ds (-> (select :%count.*)
                                     (from :ship)
                                     sql/format))]
    (get (first result) (keyword "COUNT(*)"))))

;; deletes
(defn clear-ship-data!
  "Deletes all ship data from the database, use with caution"
  []
  (jdbc/with-transaction [tx ds]
       (let [ship-attack-deletes (jdbc/execute! tx (-> (delete-from :ship-attack)
                                                       sql/format))
             ship-deletes (jdbc/execute! tx (-> (delete-from :ship)
                                                sql/format))]
         (+
           (get-update-count ship-attack-deletes)
           (get-update-count ship-deletes)))))

(defn clear-tournament-data!
  "Deletes all tournaments and lists from the database"
  []
  (jdbc/with-transaction [tx ds]
       (let [list-deletes (jdbc/execute! tx (-> (delete-from :list)
                                                sql/format))
             tournament-deletes (jdbc/execute! tx (-> (delete-from :tournament)
                                                      sql/format))]
         (+
           (get-update-count tournament-deletes)
           (get-update-count list-deletes)))))

(defn clear-list-data!
  "Deletes all lists from the database"
  []
  (get-update-count (jdbc/execute! ds (-> (delete-from :list)
                                          sql/format))))

;; inserts
(defn insert-ship!
  "Given a ship object insert it into the ship and ship attack tables. Example map
  {:faction 'first-order',
   :pilot 'kylo',
   :force 2,
   :shield 2,
   :initiative 5,
   :ship 'whisper',
   :hull 5,
   :attack [3 2], ;; attack can also just be a scalar value i.e 1
   :agility 3}
   Returns the update count
  "
  [{:keys [faction pilot ship agility initiative hull shields force attack]
          :or {shields 0 force 0 attack [0]}}]
  (jdbc/with-transaction [tx ds]
       (let [ship-results (jdbc/execute! tx (-> (insert-into :ship)
                                                (columns :faction :pilot :ship :agility :initiative :hull :shields :force)
                                                (values [[faction pilot ship agility initiative hull shields force]])
                                                sql/format))
             ship-id (:ship_id (first (jdbc/execute! tx ["select last_insert_rowid() as ship_id"])))
             ship-attack-results (jdbc/execute! tx (-> (insert-into :ship_attack)
                                                       (columns :value :ship_id)
                                                       (values (map #(vector % ship-id) (if (coll? attack) attack (vector attack))))
                                                       sql/format))]
         (+
           (get-update-count ship-attack-results)
           (get-update-count ship-results)))))

(defn insert-tournaments!
  "Inserts each tournament from a list of tournaments into the database.
  Tournaments input is of the form:
  [{:date \"2019-01-26\", :country \"NZ\", :id 261, :type 2, :format 1}
   {:date \"2019-01-26\", :country \"NZ\", :id 261, :type 2, :format 1, :num_players 5}
   {:date \"2019-01-26\", :country \"NZ\", :id 261, :type 2, :format 1}]
   Returns the number of inserted tournaments"
  [tournaments]
  (let [results (for [tournament tournaments]
                  (let [{tournament-id :tournament_id
                         type          :type
                         date          :date
                         format        :format
                         country       :country
                         num-players   :num_players} tournament]
                    (println (str "loading tournament " tournament-id))
                    (jdbc/execute! ds (-> (insert-into :tournament)
                                          (columns :tournament_id :date :format :type :county :num_players)
                                          (values [[tournament-id date format type country num-players]])
                                          sql/format))))]
    (->> results
        flatten
        (map :next.jdbc/update-count)
        (reduce +))))

(defn insert-lists!
  "Insert a list of lists into the database. Input list should be of the form
  [{}
   {}
   ...]"
  [tournament-id lists]
  (println (str "loading lists for " tournament-id))
  (let [results (for [the-list lists]
                  (let [{:keys [faction list_json swiss_rank cut_rank]} the-list]
                    (jdbc/execute! ds (-> (insert-into :list)
                                          (columns :tournament_id :faction :list_xws :swiss_rank :top_cut_rank)
                                          (values [[tournament-id faction list_json swiss_rank cut_rank]])
                                          sql/format))))]
    (->> results
         flatten
         (map :next.jdbc/update-count)
         (reduce +))))

;; updates
(defn update-participant-count!
  "Update the number of participants for a tournament"
  [tournament-id participant-count]
  (let [results (jdbc/execute! ds (-> (h/update :tournament)
                                      (h/set {:num_players participant-count})
                                      (where [:= :tournament_id tournament-id])
                                      sql/format))]
    (:next.jdbc/update-count (first results))))
