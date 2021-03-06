(ns monkeycoding.editor.timeline.wave
    (:require
      [reagent.core :as r]
      [monkeycoding.widgets.util :refer [as-component]]
      [monkeycoding.editor.timeline.widgets  :refer [timeline-progress timeline-pin collapsible-v-panel]]))


;; later can be dynamic to control zoom
(def ms-to-px-ratio 20)
(def segement-ms 200)
(def look-ahead-ms 3000)

(def wave-height-px 75)
(def timeline-container-height-px 150)


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
    (cond
        (empty? segements) nil
        (> 0 position)     nil
        :otherwise         (:index (nth segements position)))))


(defn px->position [segements px]
  (let [
        px-index (.floor js/Math (* ms-to-px-ratio (/ px segement-ms)))
        clamped-index (min px-index (dec (count segements)))]

    (loop [index clamped-index]
        (cond
          (= 0 index)                     (get-in segements [0 0 :input-index])
          (empty? (nth segements index))  (recur (dec index))
          :otherwise                      (get-in segements [index 0 :input-index])))))


(defn segments->width-px [segements]
  (let [
        window-width  (.-innerWidth js/window)
        segement-width (/ (* (count segements) segement-ms) ms-to-px-ratio)
        look-ahead-pad (/ look-ahead-ms ms-to-px-ratio)]

    (max window-width (+ segement-width look-ahead-pad))))


(defn init-wave! [canvas]
    {
      :canvas canvas
      :ctx    (.getContext canvas "2d")
      :width  0
      :height 0})


(defn adjust-wave-size! [{:keys [canvas ctx] :as wave} segements] nil
  (set! (.-height canvas) wave-height-px)
  (set! (.-width canvas) (segments->width-px segements))

  (assoc wave :width  (.-width (.getBoundingClientRect canvas))
              :height (.-height (.getBoundingClientRect canvas))))


(defn render-wave! [{:keys [ctx width height] :as wave}  segements]

  (.clearRect ctx 0 0 width height)
  (aset ctx "fillStyle" "#006495")

  (doseq [[current index] (map vector segements (range))]
    (let [
          h (* height (min 0.8 (+ 0.25 (* 0.15 (dec (count current))))))
          w (/ (- segement-ms 20) ms-to-px-ratio)
          x (/ (* index segement-ms) ms-to-px-ratio)
          y (- height h)
          [r -r] [0 0]]

      (when-not (empty? current)
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
          (.fill)))))

  wave)


(defn- sync-wave! [wave segements]
  (-> wave
    (adjust-wave-size! segements)
    (render-wave! segements)))


(defn wave-canvas [{:keys [
                            position
                            segements]}]
  (let [the-wave   (r/atom nil)]
    (as-component {
                      :on-mount (fn [this]
                                  (reset! the-wave
                                      (-> (r/dom-node this)
                                        (init-wave!)
                                        (sync-wave! segements))))

                      :on-props (fn [{:keys [position, segements]}]
                                    (swap! the-wave sync-wave!  segements))
                      :render (fn [] [:canvas])})))


(defn wave-progress [{:keys [
                                inputs
                                open
                                on-seek
                                position]}]
  (let [
        segements            (inputs->wave-segements inputs)
        active-segment-index (position->segment-index segements position)
        width                (segments->width-px segements)
        progress-px          (/ (* active-segment-index segement-ms) ms-to-px-ratio)]

    [timeline-progress {
                        :class "wave-progress"
                        :on-seek #(when-let [index (px->position segements %2)] (on-seek index))
                        :progress [width progress-px]
                        :open open}]))


(defn wave-panel [{:keys [
                                inputs
                                marks
                                open
                                on-seek
                                position]}]
  (let [
        segements            (inputs->wave-segements inputs)
        active-segment-index (position->segment-index segements position)
        width (segments->width-px segements)]

    [collapsible-v-panel {:show open :style {
                                              :width width
                                              :max-height timeline-container-height-px}}
      [:div
       [:div.timeline-pins {:class (when-not open "hidden")}
          (let [
                marks (->> (vals marks)
                        (remove #(= -1 (:inserted-at %)))
                        (group-by :inserted-at))
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
                                      :on-click #(on-seek position)}]))))]
       [wave-canvas {
                     :segements segements
                     :position  active-segment-index}]]]))
