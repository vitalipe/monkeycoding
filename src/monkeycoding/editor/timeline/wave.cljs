(ns monkeycoding.editor.timeline.wave
    (:require
      [reagent.core :as r]
      [monkeycoding.editor.common            :refer [as-component]]
      [monkeycoding.editor.timeline.widgets  :refer [timeline-progress scroll-panel collapsible-v-panel]]))


;; later can be dynamic to control zoom
(def ms-to-px-ratio 10)
(def segement-ms 80)
(def look-ahead-ms 3000)


(defn inputs->wave-segements [inputs]
  (loop [[first, & rest] inputs, segements [[]], delay (get-in inputs [0 :dt])]
    (let [
          next-delay (+ delay (:dt first))
          seg-index (dec (count segements))
          segements-time (* segement-ms (count segements))]
      (cond
        (nil? first)                  segements
        (> next-delay segements-time) (recur (into [first] rest) (conj segements []) delay)
        :otherwise                    (recur rest (update segements seg-index conj first) next-delay)))))


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


(defn render-wave! [{:keys [ctx width height]}  segements]
  (.clearRect ctx 0 0 width height)

  (doseq [[current index] (map vector segements (range))]
    (let [
          h (* height (min 0.6, (* 0.15 (* 1.2 (count current)))))
          w (/ (- segement-ms 20) ms-to-px-ratio)
          x (/ (* index segement-ms) ms-to-px-ratio)
          y (- (/ height 2) (/ h 2))]

      (doto ctx
        (.beginPath)
        (.moveTo           (+ x 4),    y)
        (.lineTo           (+ x w -4), y)
        (.quadraticCurveTo (+ x w),    y,            (+ x w),    (+ y 4))
        (.lineTo           (+ x w),    (+ y h -4))
        (.quadraticCurveTo (+ x w),    (+ y h)       (+ x w -4), (+ y h))
        (.lineTo           (+ x 4),    (+ y h))
        (.quadraticCurveTo x,          (+ y h)       x,          (+ y h -4))
        (.lineTo           x,          (+ y 4))
        (.quadraticCurveTo x,          y             (+ x 4),    y)
        (.closePath)

        (aset "fillStyle" "#006495")
        (.fill)))))


(defn- sync-wave! [wave segements]
  (doto (adjust-wave-size! wave segements)
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



(defn wave-progress [{:keys [open segements position width]}]
  [timeline-progress {
                      :progress [width width]
                      :open open}])


(defn wave-panel [{:keys [
                                inputs
                                open
                                on-seek
                                position]}]

    (r/with-let [state (r/atom {:width-px 0})]
      (let [segements (inputs->wave-segements inputs)]
        [:div.timeline-container
          [collapsible-v-panel {:show open}
            [scroll-panel
                [wave-progress {
                                :open open
                                :width (:width-px @state)
                                :segements segements}]
                [wave-canvas {
                              :segements segements
                              :position  position
                              :on-resize #(swap! state assoc :width-px %)}]]]])))
