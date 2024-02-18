(ns analysis.stats
  (:require
    [analysis.db :as db]
    [analysis.constants :as constants]
    [cheshire.core :refer :all]
    [java-time.api :as jt]))

(defn avg
  "Average a specific stat for a list of ships"
  [ships stat]
  (if (= (count ships) 0)
    0                                                       ;; some kind of weird edge case with ships in certain lists?
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

(defn avg-lists
  [list-stats]
  (into {} (for [k (keys (first list-stats))
                 :when (not= :date k)]
             [k (/ (reduce + (map #(get % k) list-stats)) (count list-stats))])))

(defn jt-between?
  "Return true iff test date is before end and after start (inclusive)."
  [start end test]
  (jt/before? (jt/minus start (jt/days 1)) test (jt/plus end (jt/days 1))))

(defn get-lists-in-range
  "For a date range, return the lists in that range as a flattened array"
  [start-date end-date lists-by-date]
  (filter #(jt-between? start-date (first %) end-date) lists-by-date))

(defn sliding-window-avg-lists
  "loop recur here, incrementing the start and end date windows by 1 each time until we reach the end
  to produce a single data point for each day
  list-stats-by-date should be grouped by date (i.e. keys are like 2023-11-04) and the values are an
  array of lists-stats (computed from the analyze-list function)

  start-index/end-index keeps the indices of the dates of the first/last groups in the window, so that we can
  check the 'next' indices to see if they are now in/out of the new date range
  acc keeps the sum of data points we've averaged so far and the total list counts

  start the loop at start-date + window-size / 2
  break the loop when window-end-date > termination-date - the window size/2
  when breaking the loop, if we're the top level, divide stats by count"
  [start-date end-date list-stats window-size]
  (let [list-stats-by-date (update-keys (group-by :date list-stats) jt/local-date)
        window-half-size (/ window-size 2)
        start-date-window (jt/plus start-date (jt/days window-half-size))
        end-date-window (jt/plus start-date-window (jt/days window-size))
        loop-termination-date (jt/minus end-date (jt/days window-half-size))]
    (loop [l-start-date-window start-date-window
           l-end-date-window end-date-window
           acc '()]
      (let [lists (get-lists-in-range l-start-date-window l-end-date-window list-stats-by-date)
            result (avg-lists lists)
            next-start-date-window (jt/plus l-start-date-window (jt/days 1))
            next-end-date-window (jt/plus l-start-date-window (jt/days 1))]
        (if (jt/before? next-end-date-window loop-termination-date)
          (recur next-start-date-window next-end-date-window (cons result acc))
          acc)))))

(defn analyze-list
  "Pull out various stats from a single list"
  [{:keys [faction pilots date]}]
  (let [ships (for [{ship :ship pilot :id} pilots]
                (db/get-ship-by-id-memo faction ship pilot))]
    {:date             date
     :avg-attack       (avg ships :attack)
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
  [start-date end-date window lists]
  (let [list-stats (map analyze-list lists)]
    {:time-series (sliding-window-avg-lists start-date end-date list-stats window)
     :overall     (avg-lists list-stats)}))




(defn analyze
  "Return meta analysis for all lists within the given date range. The returned
  analysis includes all faction and per faction stats. Optionally filter for tournaments with
  at least a minimum number of players, and allowing specifying a custom window size for rolling averages."
  [start-date end-date & {:keys [min-players window] :or {min-players 0 window 30}}]
  (let [factions (map name constants/factions)
        lists (->> (db/get-lists factions start-date end-date min-players)
                   (map (fn [lst] (merge {:date (:date lst)} (parse-string (:list_xws lst) true)))))
         lists-by-faction (merge {:all lists} (update-keys (group-by :faction lists) keyword))]
    (update-vals lists-by-faction (partial analyze-lists start-date end-date window))))
