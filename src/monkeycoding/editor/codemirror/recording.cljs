(ns monkeycoding.editor.codemirror.recording)


(defn- extract-position [js-pos]
  {:line (.-line js-pos) :ch (.-ch js-pos)})


(defn- cm-input-data->event [text event dt]
  (let [
        input  (js->clj event)
        origin (get input "origin")

        cursor-event? (nil? origin)
        selection-event? (contains? input "ranges")
        text-event? (contains? #{"+input" "+delete" "cut" "paste" "copy" "undo" "redo"} origin)]
    (merge {
              :dt dt
              :snapshot text}

          (cond
              selection-event? {
                                :type :selection
                                :head   (extract-position (.-head (first (input "ranges"))))
                                :anchor (extract-position (.-anchor (first (input "ranges"))))}

            cursor-event?  {
                            :type :cursor
                            :position (extract-position event)}

            text-event?    {
                             :type :input
                             :insert (clojure.string/join "\n" (input "text"))
                             :remove (count (clojure.string/join "\n" (input "removed")))
                             :position (extract-position (get input "from"))}))))


(defn- selecting-now? [{:keys [last last-selection]}]
  (and
    (not= (:head last-selection) (:anchor last-selection))
    (= last last-selection)))


(defn- redundant-event? [state current prv]
  (or
    (and (= (current :type) :cursor) (selecting-now? state))
    (nil? (:type current))
    (= (dissoc current :dt) (dissoc prv :dt))))


(defn- dt [now last-time]
  (if last-time (- now last-time) 0))


(defn- take-snapshot [codemirror]
  (.getValue codemirror))


(defn- merge-input-data [{last-time :last-time :as state}, text input]
  (let [
        now       (.now js/Date)
        event     (cm-input-data->event text input (dt now last-time))
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
