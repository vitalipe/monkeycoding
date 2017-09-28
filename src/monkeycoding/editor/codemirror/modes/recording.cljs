(ns monkeycoding.editor.codemirror.modes.recording
  (:require
    [monkeycoding.editor.codemirror.snapshot   :as snapshot]))


(defn text->position [text]
  {
    :line (count (re-seq #"\n" text))
    :ch (count (last (clojure.string/split text #"\n")))})


(defn- position-after-input [input]
  (cond
    (empty? (:insert input)) (:position input)
    :otherwise (merge-with + (:position input) (text->position (:insert input)))))


(defn- selecting-now? [{:keys [last last-selection]}]
  (and
    (not= (:to last-selection) (:from last-selection))
    (= last last-selection)))


(defn- empty-selection? [{:keys [type from to]}]
  (and
      (= type type :selection)
      (= from to)))


(defn- cursor-event-during-selection? [state {type :type}]
  (and
    (= type :cursor)
    (selecting-now? state)))


(defn- shadow-cursor-after-input? [cur prv]
  (and
    (= (:type cur) :cursor)
    (= (:type prv) :input)
    (= (cur :position) (position-after-input prv))))


(defn- empty-isolated-selection? [selection prv]
  (and
    (not= (:type prv) :selection)
    (empty-selection? selection)))


(defn- redundant-event? [state current prv prv-of-same-type]
    (or
      (empty-isolated-selection? current prv)
      (cursor-event-during-selection? state current)
      (shadow-cursor-after-input?  current prv)
      (= current prv)))


(defn- calc-dt [now last-time]
  (if last-time (- now last-time) 0))


(defn take-adjusted-snapshot [cm marks {type :type :as event}]
  (let [snapshot (snapshot/take-snapshot cm marks)]
    (cond
      (= type :selection) (assoc snapshot :selection (select-keys event [:from :to]))
      :otherwise snapshot)))


(defn- merge-input-data [state input now]
  (let [
        target    (if (= (input :type) :selection) :last-selection :last-input)
        redundant (redundant-event? state input (:last state) (get state target))]

    (merge state
        (when-not redundant {
                              :last-time now
                              :last input
                               target input}))))


;; lifesycle
(defn process-input-event [{:keys [last-time on-input marks] :as state} cm event]
  (let [
        now  (.now js/Date)
        dt (calc-dt now last-time)
        snapshot (take-adjusted-snapshot cm marks event)
        new-state (merge-input-data state event now)
        changed   (not= state new-state)]

    (when changed
      (on-input (:last new-state) snapshot dt))

    new-state))


(defn sync-with-props! [this _  props]
  (merge this (select-keys props [:on-input :marks])))

(defn enter! [this cm props]
  (snapshot/apply-snapshot! cm (select-keys props [:selection :text :marks]))
  (sync-with-props! this cm props))
