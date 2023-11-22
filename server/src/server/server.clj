(ns server.server
  (:require [analysis.stats :as stats]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.walmartlabs.lacinia.pedestal2 :as lp]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :as util]
            [io.pedestal.http :as http]))

(def rename-keymap
  {:avg-agility      :agility
   :avg-attack       :attack
   :avg-force        :force
   :avg-hull         :hull
   :avg-initiative   :initiative
   :avg-shields      :shields
   :num-ships        :numShips
   :total-agility    :totalAgility
   :total-attack     :totalAttack
   :total-force      :totalForce
   :total-health     :totalHealth
   :total-hull       :totalHull
   :total-initiative :totalInitiative
   :total-shields    :totalShields})

(defn in?
  "true if coll contains elm"
  [coll elm]
  (some #(= elm %) coll))

(defn convert-to-faction-analysis
  "Take the raw result from the stats analysis and convert it to the graphql format we want
  for each faction. The input data is the map entry with the key being the faction and the value
  being the :overall and :time-series data"
  [[faction data]]
  {:faction    faction
   :overall    (clojure.set/rename-keys (:overall data) rename-keymap)
   :timeSeries (clojure.set/rename-keys (:time-series data) rename-keymap)})

(defn resolve-meta-analysis
  [context args value]
  (let [factions (:factions args)
        stats (stats/analyze (:startDate args) (:endDate args))]
    (filter #(in? factions (:faction %)) (map convert-to-faction-analysis stats))))


(defn resolver-map
  []
  {:Query/metaAnalysis resolve-meta-analysis})


(defn analysis-schema
  []
  (-> (io/resource "schema.edn")
      slurp
      edn/read-string
      (util/inject-resolvers (resolver-map))
      schema/compile))

(defonce server nil)

(defn start-server
  [_]
  (let [server (-> (lp/default-service (analysis-schema) nil)
                   http/create-server
                   http/start)]
    server))

(defn stop-server
  [server]
  (http/stop server)
  nil)

(defn start
  []
  (alter-var-root #'server start-server)
  :started)

(defn stop
  []
  (alter-var-root #'server stop-server)
  :stopped)

(defn restart
  []
  (stop)
  (start))
