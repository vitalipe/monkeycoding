(ns monkeycoding.editor.timeline
    (:require
      [reagent.core :as r]
      [monkeycoding.widgets.scroll  :refer [drag-scroll-panel]]
      [monkeycoding.editor.timeline.widgets  :refer [timeline-progress timeline-pin]]
      [monkeycoding.editor.timeline.wave     :refer [wave-progress wave-panel]]))


(defn- calc-% [a b]
  (* 100 (when-not (zero? a) (/ b a))))


(defn timeline-pins [{:keys [
                             inputs
                             marks
                             open
                             position
                             on-seek]}]
  (let [
        input-size (count inputs)
        pins (group-by :insert (vals marks))]
    [:div.timeline-pins {:class (when-not open "hidden")}
       (->> pins
         (map (fn [[position all-marks-in-pos]]
                [timeline-pin {
                                :key position
                                :position (str (calc-% input-size (inc position)) "%")
                                :on-click #(on-seek position)
                                :count (count all-marks-in-pos)}])))]))


(defn percentage-progress [{:keys [open
                                   inputs
                                   position
                                   on-seek]}]

  (r/with-let [
                percentage->position #(max 0 (dec (.round js/Math (* %1 (/ %2 100)))))
                state (r/atom {
                                :last-size (count inputs)
                                :last-progress (calc-% (count inputs) (inc position))
                                :last-position 0})]

    (let [input-size (count inputs)]
      [timeline-progress {
                          :class "percentage-progress"
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
                                                            :last-position position})))}])))


(defn timeline-panel [{:keys [
                                stream
                                open
                                on-seek
                                position]}]

      [drag-scroll-panel {:scrollable open 
                          :class :timeline-container}
        [:div.data-area
         [timeline-pins {
                         :inputs (:inputs stream)
                         :marks  (:marks stream)
                         :position position
                         :on-seek on-seek
                         :open (not open)}]
         [wave-panel
                     {:open open
                      :inputs (:inputs stream)
                      :marks  (:marks stream)
                      :position position
                      :on-seek on-seek}]]

        [:div.progress-area
          [percentage-progress {
                                :inputs (:inputs stream)
                                :open (not open)
                                :position position
                                :on-seek on-seek}]
          [wave-progress {
                          :inputs (:inputs stream)
                          :marks  (:marks stream)
                          :open open
                          :position position
                          :on-seek on-seek}]]])
