(ns analysis.stats
  (:require
    [analysis.db :as db]
    [analysis.constants :as constants]
    [cheshire.core :refer :all]))

(defn avg
  "Average a specific stat for a list of ships"
  [ships stat]
  (if (= (count ships) 0)
    0 ;; some kind of weird edge case with ships in certain lists?
    (let [sum (->> ships
                   (map stat)
                   flatten
                   (reduce +))]
      (float (/ sum (count ships))))))

(defn total
  "Total a specific stat for a list of ships"
  [ships stat]
  (->> ships
       (map stat)
       flatten
       (reduce +)
       float))

;; I'm not smart enough to have written this, but it's very elegant
;; https://stackoverflow.com/questions/2359821/sequence-of-a-rolling-average-in-clojure
(defn sliding-window-moving-average [window lst]
  (map #(/ % window)
       (let [start   (apply + (take window lst))
             diffseq (map - (drop window lst) lst)]
         (reductions + start diffseq))))

(defn analyze-list
  "Pull out various stats from a single list"
  [{:keys [faction pilots]}]
  (let [ships    (for [{ship :ship pilot :id} pilots]
                   (db/get-ship-by-id-memo faction ship pilot))]
     {:avg-attack       (avg ships :attack)
      :avg-agility      (avg ships :agility)
      :avg-hull         (avg ships :hull)
      :avg-shields      (avg ships :shields)
      :avg-initiative   (avg ships :initiative)
      :avg-force        (avg ships :force)
      :total-attack     (total ships :attack)
      :total-agility    (total ships :agility)
      :total-hull       (total ships :hull)
      :total-shields    (total ships :shields)
      :total-initiative (total ships :initiative)
      :total-force      (total ships :force)
      :total-health     (+ (total ships :hull) (total ships :shields))
      :num-ships        (count ships)}))

(defn analyze-lists
  "For a set of lists, get the stats for each list, and average them."
  [window lists]
  (let [list-stats (map analyze-list lists)]
    {:time-series (into {} (for [k (keys (first list-stats))]
                             [k (sliding-window-moving-average window (map #(get % k) list-stats))]))
     :overall     (into {} (for [k (keys (first list-stats))]
                             [k (/ (reduce + (map #(get % k) list-stats)) (count list-stats))]))}))




(defn analyze
  "Return meta analysis for all lists within the given date range. The returned
  analysis includes all faction and per faction stats. Optionally filter for tournaments with
  at least a minimum number of players, and allowing specifying a custom window size for rolling averages."
 [start end & {:keys [min-players window] :or {min-players 0 window 30}}]
 (let [factions (map name constants/factions)
       lists (->> (db/get-lists factions start end min-players)
                  (map :list_xws)
                  (map #(parse-string % true)))
        lists-by-faction (merge {:all lists} (update-keys (group-by :faction lists) keyword))]
   (update-vals lists-by-faction (partial analyze-lists window))))
