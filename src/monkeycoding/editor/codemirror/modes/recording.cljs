(ns monkeycoding.editor.codemirror.modes.recording
  (:require
    [monkeycoding.editor.codemirror.snapshot   :as snapshot]))


(defn- position-after-input [{ {:keys [ch line]} :position  text :insert}]
  (let [
        additional-rows  (count (re-seq #"\n" text))
        additional-chars (count (last (clojure.string/split  text #"\n")))]
    {
      :line (+ additional-rows line)
      :ch   (+ additional-chars (if (= additional-rows 0) ch 0))}))


(defn- empty-selection? [{:keys [type from to]}]
  (and
      (= type type :selection)
      (= from to)))


(defn- cursor-event-during-selection? [selecting {type :type}]
  (and selecting (= type :cursor)))


(defn- shadow-cursor-after-input? [cur prv]

  (and
    (= (:type cur) :cursor)
    (= (:type prv) :input)
    (= (cur :position) (position-after-input prv))))


(defn- redundant-event? [{:keys [last selecting]} current]
    (or
      (empty-selection? current)
      (cursor-event-during-selection? selecting current)
      (shadow-cursor-after-input?  current last)
      (= current last)))


(defn- calc-dt
        ([now last] (calc-dt now last (* 999  1000)))
        ([now last dt-cap] (min dt-cap (if last (- now last) 0))))


(defn take-adjusted-snapshot [cm marks {type :type :as event}]
  (let [snapshot (snapshot/take-snapshot cm marks)]
    (cond
      (= type :selection) (assoc snapshot :selection (select-keys event [:from :to]))
      :otherwise snapshot)))


(defn- merge-input-data [state input now]
  (-> state
      (assoc :selecting (and
                          (= :selection (:type input))
                          (not (empty-selection? input))))

      (merge (when-not (redundant-event? state input) {:last-time now :last input}))))


;; lifesycle
(defn process-input-event [{:keys [last-time on-input marks dt-cap] :as state} cm event]
  (let [
        now  (.now js/Date)
        dt (calc-dt now last-time dt-cap)
        snapshot (take-adjusted-snapshot cm marks event)
        new-state (merge-input-data state event now)
        changed   (not= (:last state) (:last new-state))]

    (when changed
      (on-input (:last new-state) snapshot dt))

    new-state))


(defn sync-with-props! [this _  props]
  (-> this
    (assoc :last-time (.now js/Date))
    (merge (select-keys props [:on-input :marks :dt-cap]))))



(defn enter! [this cm props]
  (snapshot/apply-snapshot! cm (select-keys props [:selection :text :marks]))
  (sync-with-props! this cm props))
