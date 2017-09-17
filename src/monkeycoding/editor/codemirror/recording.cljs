(ns monkeycoding.editor.codemirror.recording)


(defn- selecting-now? [{:keys [last last-selection]}]
  (and
    (not= (:head last-selection) (:anchor last-selection))
    (= last last-selection)))


(defn- redundant-event? [state current prv]
  (or
    (and (= (current :type) :cursor) (selecting-now? state))
    (= (dissoc current :dt) (dissoc prv :dt))))


(defn- dt [now last-time]
  (if last-time (- now last-time) 0))


(defn- take-snapshot [codemirror]
  (.getValue codemirror))


(defn- merge-input-data [{last-time :last-time :as state} snapshot input]
  (let [
        now       (.now js/Date)
        event     (merge input  {:dt (dt now last-time) :snapshot snapshot})
        target    (if (= (event :type) :selection) :last-selection :last-input)
        redundant (redundant-event? state event (get state target))]

    (merge state
        (when-not redundant {
                              :last-time now
                              :last event
                               target event}))))


(defn process-input-event [{on-input :on-input :as state} cm event]
  (let [
        snapshot  (take-snapshot cm)
        new-state (merge-input-data state snapshot event)
        changed   (not= state new-state)]

    (when changed
      (on-input (:last new-state)))

    new-state))
