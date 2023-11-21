(ns analysis.constants)

(def tournament-types
  {1 :store-event
   2 :national-championship
   3 :hyperspace-trial
   4 :hyperspace-cup
   5 :system-open
   6 :world-championship
   7 :casual-event
   8 :other})

(def formats
  {1  :2.0-extended
   2  :2.0-standard
   3  :custom
   4  :other
   34 :2.0-hyperspace
   35 :2.5-extended
   36 :2.5-standard
   37 :2.0-legacy-standard
   38 :2.0-legacy-wild-space})

(def stats [:hull :shield :attack :agility :initiative :force])
(def factions [:galacticrepublic :galacticempire :firstorder :rebelalliance :resistance :scumandvillainy :separatistalliance])

;; seems like there's some inconsistencies between naming with xws and the data we loaded.
;; Broken out by faction (in case of duplicates) then pilot name, keys are alternate
;; pilot names and the values are the canonical name (according to our db/xwing-data2)
(def pilot-alternate-name-mappings
  {:rebelalliance      {:yt2400lightfreighter         {:dashrendar-swz103 "dashrendar-swz103-rebelalliance"
                                                       :leebo-swz103      "leebo-swz103-rebelalliance"}
                        :modifiedyt1300lightfreighter {:hansolo "hansolo-modifiedyt1300lightfreighter"}
                        :fangfighter                  {:fennrau-rebelfangfighter "fennrau-fangfighter"}
                        :rz1awing                     {:herasyndullaawing "herasyndulla-rz1awing"}
                        :t65xwing                     {:lukeskywalkerboy "lukeskywalker-battleofyavin"}}

   :scumandvillainy    {:fangfighter               {:fennrau-rebel-fang "fennrau-fangfighter"}
                        :rogueclassstarfighter     {:cadbane-rogue "cadbane"}
                        :firesprayclasspatrolcraft {:hondoohnaka-firesprayclasspatrolcraft "hondoohnaka"}
                        :z95af4headhunter          {:bossk "bossk-z95af4headhunter"}}

   :separatistalliance {:firesprayclasspatrolcraft {:bobafett-separatistalliance "bobafett-firesprayclasspatrolcraft"}
                        :gauntletfighter           {:bokatankryze "bokatankryze-separatistalliance"
                                                    :bokatankryze-separatist "bokatankryze-separatistalliance"}}

   :firstorder         {:tiefofighter                    {:dt798-tiefofighter "dt798"}
                        :tiewiwhispermodifiedinterceptor {:kyloren "kyloren-tiewiwhispermodifiedinterceptor"}}

   :galacticempire     {:tielnfighter {:maulermither-battleofyavin "maulermithel-battleofyavin"}
                        :tieddefender {:darthvader "darthvader-tieddefender"}}

   :galacticrepublic   {:v19torrentstarfighter {:kickback-sigeofcoruscant "kickback-siegeofcoruscant"}
                        :delta7baethersprite   {:macewindu "macewindu-delta7baethersprite"
                                                :obiwankenobi "obiwankenobi-delta7baethersprite"
                                                :ahsokatano "ahsokatano-delta7baethersprite"
                                                :barrissoffee "barrissoffee-delta7baethersprite"}}})




