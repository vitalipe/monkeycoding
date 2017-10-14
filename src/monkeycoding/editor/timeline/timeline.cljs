(ns monkeycoding.editor.timeline
    (:require
      [reagent.core :as r]
      [monkeycoding.editor.timeline.widgets  :refer [timeline-progress timeline-pin]]
      [monkeycoding.editor.timeline.wave     :refer [wave-panel]]))



(defn percentage-progress [{:keys [open
                                   inputs
                                   marks
                                   position
                                   on-seek]}]

  (r/with-let [
                calc-% #(* 100 (when-not (zero? %1) (/ %2 %1)))
                percentage->position #(max 0 (dec (.round js/Math (* %1 (/ %2 100)))))
                state (r/atom {
                                :last-size (count inputs)
                                :last-progress (calc-% (count inputs) (inc position))
                                :last-position 0})]


    (let [input-size (count inputs)]
      [timeline-progress {
                          :open open
                          :progress (cond
                                      (not= input-size (:last-size @state))   (calc-% input-size (inc position))
                                      (not= position (:last-position @state)) (calc-% input-size (inc position))
                                      :otherwise                              (:last-progress @state))
                          :on-seek (fn [total complete]
                                      (let [
                                            progress (calc-%  total complete)
                                            position (percentage->position input-size progress)]
                                        (on-seek position)
                                        (swap! state merge {
                                                            :last-size input-size
                                                            :last-progress progress
                                                            :last-position position})))}
          (->> (vals marks)
            (group-by :insert)
            (map (fn [[position all-marks-in-pos]]
                    [timeline-pin {
                                    :key position
                                    :position (str (calc-% input-size position) "%")
                                    :on-click #(on-seek position)
                                    :count (count all-marks-in-pos)}])))])))

(defn timeline-panel [{:keys [
                                stream
                                open
                                on-seek
                                position]}]

      [:div.timeline-container
        [percentage-progress {
                              :inputs (:inputs stream)
                              :marks  (:marks stream)
                              :open (not open)
                              :position position
                              :on-seek on-seek}]
        [wave-panel
                    {:open open
                     :inputs (:inputs stream)
                     :marks  (:marks stream)
                     :position position
                     :on-seek on-seek}]])
