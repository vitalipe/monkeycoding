(ns monkeycoding.editor.timeline.wave
    (:require
      [reagent.core :as r]
      [monkeycoding.util :refer [as-component]]
      [monkeycoding.editor.timeline.widgets  :refer [timeline-progress timeline-pin scroll-panel collapsible-v-panel]]))


;; later can be dynamic to control zoom
(def ms-to-px-ratio 10)
(def segement-ms 80)
(def look-ahead-ms 3000)


(defn inputs->wave-segements [inputs]
  (let [into-segements (fn [segements item items-left]
                          (let [
                                index (dec (count segements))
                                input-index (- (count inputs) items-left 1)]

                            (update segements index conj {
                                                          :input-index input-index
                                                          :index index
                                                          :input item})))]

    (loop [[first, & rest] inputs, segements [[]], delay (get-in inputs [0 :dt])]
        (let [
              next-delay (+ delay (:dt first))
              segements-time (* segement-ms (count segements))]

          (cond
            (nil? first)                  segements
            (> next-delay segements-time) (recur (into [first] rest) (conj segements []) delay)
            :otherwise                    (recur rest (into-segements segements first (count rest)) next-delay))))))


(defn position->segment-index [segements position]
  (let [segements (flatten segements)]
    (when-not (empty? segements)
        (:index (nth segements position) position))))


(defn px->position [segements px]
  (let [
        px-index (.floor js/Math (* ms-to-px-ratio (/ px segement-ms)))
        clamped-index (min px-index (dec (count segements)))]

    (loop [index clamped-index]
        (cond
          (= 0 index)                     (get-in segements [0 0 :input-index])
          (empty? (nth segements index))  (recur (dec index))
          :otherwise                      (get-in segements [index 0 :input-index])))))



(defn init-wave! [canvas]
    {
      :canvas canvas
      :ctx    (.getContext canvas "2d")
      :width  0
      :height 0})


(defn adjust-wave-size! [{:keys [canvas ctx] :as wave} segements] nil
  (let [
        parent-width  (.-width (.getBoundingClientRect (.-parentElement canvas)))
        segement-width (/ (* (count segements) segement-ms) ms-to-px-ratio)
        look-ahead-pad (/ look-ahead-ms ms-to-px-ratio)]

    (set! (.-width canvas) (max parent-width (+ segement-width look-ahead-pad)))
    ;; this hack should disable sub-pixel AA
    ;; setting canvas size will reset the transform matrix, so we do this for every size change..
    (.translate ctx 0.5 0.5))

  (merge wave {
                :width  (.-width (.getBoundingClientRect canvas))
                :height (.-height (.getBoundingClientRect canvas))}))


(defn render-wave! [{:keys [ctx width height] :as wave}  segements]
  (.clearRect ctx 0 0 width height)
  (doseq [[current index] (map vector segements (range))]
    (let [
          h (* height (min 0.6, (* 0.15 (* 1.2 (count current)))))
          w (/ (- segement-ms 20) ms-to-px-ratio)
          x (/ (* index segement-ms) ms-to-px-ratio)
          y (- (/ height 2) (/ h 2))
          [r -r] (if (empty? current) [1 -2] [4 -4])]

      (doto ctx
        (.beginPath)
        (.moveTo           (+ x r),    y)
        (.lineTo           (+ x w -r), y)
        (.quadraticCurveTo (+ x w),    y,            (+ x w),    (+ y r))
        (.lineTo           (+ x w),    (+ y h -r))
        (.quadraticCurveTo (+ x w),    (+ y h)       (+ x w -r), (+ y h))
        (.lineTo           (+ x r),    (+ y h))
        (.quadraticCurveTo x,          (+ y h)       x,          (+ y h -r))
        (.lineTo           x,          (+ y r))
        (.quadraticCurveTo x,          y             (+ x r),    y)
        (.closePath)

        (aset "fillStyle" "#006495")
        (.fill))))

  wave)


(defn- sync-wave! [wave segements]
  (-> wave
    (adjust-wave-size! segements)
    (render-wave! segements)))


(defn wave-canvas [{:keys [
                            position
                            segements
                            on-resize]}]

  (let [
        the-wave   (r/atom nil)]

    (as-component {
                      :on-mount (fn [this]
                                  (reset! the-wave
                                      (-> (r/dom-node this)
                                        (init-wave!)
                                        (sync-wave! segements)))
                                  (on-resize (:width @the-wave)))

                      :on-props (fn [{:keys [position, segements]}]
                                    (swap! the-wave sync-wave!  segements)
                                    (on-resize (:width @the-wave)))

                      :render (fn [] [:canvas])})))



(defn wave-progress [{:keys [
                              open
                              segements
                              marks
                              on-seek
                              position
                              width]}]

  [timeline-progress {
                      :on-seek #(when-let [index (px->position segements %2)] (on-seek index))
                      :progress [width (/ (* position segement-ms) ms-to-px-ratio)]
                      :open open}

                    (let [
                          marks (group-by :insert (vals marks))
                          segements (->> (flatten segements)
                                      (sort-by :input-index)
                                      (map :index)
                                      (into []))]

                      (->> marks
                        (map (fn [[position all-marks-in-pos]]
                                [timeline-pin {
                                                :key position
                                                :count (count all-marks-in-pos)
                                                :position (/ (* (nth segements position) segement-ms) ms-to-px-ratio)
                                                :on-click #(on-seek position)}]))))])

(defn wave-panel [{:keys [
                                inputs
                                marks
                                open
                                on-seek
                                position]}]

    (r/with-let [state (r/atom {:width-px 0})]
      (let [
            segements            (inputs->wave-segements inputs)
            active-segment-index (position->segment-index segements position)]

        [:div.timeline-container
          [collapsible-v-panel {:show open}
            [scroll-panel
                [wave-progress {
                                :open open
                                :width (:width-px @state)
                                :position active-segment-index
                                :segements segements
                                :marks marks
                                :on-seek on-seek}]
                [wave-canvas {
                              :segements segements
                              :position  active-segment-index
                              :on-resize #(swap! state assoc :width-px %)}]]]])))
