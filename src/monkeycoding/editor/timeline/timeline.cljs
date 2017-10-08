(ns monkeycoding.editor.timeline
    (:require
      [reagent.core :as r]
      [monkeycoding.editor.common        :refer [as-component]]
      [monkeycoding.editor.timeline.wave :as wave]))


(defn wave-widget [{:keys [
                            position
                            stream
                            on-position-change]}]


  (let [
        the-wave (r/atom nil)
        prv-stream (r/atom nil)]

    (as-component {
                      :on-mount (fn [this]
                                  (reset! the-wave (new wave/Wave (r/dom-node this)))
                                  (reset! prv-stream stream)
                                  (doto @the-wave
                                    (.setStream (clj->js (:inputs stream)))
                                    (.render)))


                      :on-props (fn [{:keys [position, stream, on-position-change]}]
                                  (when (not= stream @prv-stream)
                                    (doto @the-wave
                                      (.setStream (clj->js (:inputs stream)))
                                      (.render)))

                                  (reset! prv-stream stream))


                      :render (fn [] [:canvas])})))



(defn timeline-widget [{:keys [
                                stream
                                position]}]
  (let [
        stream-length (count (:inputs stream))
        stream-index  (if (> stream-length 0) (inc position) 0)]
    [:div.timeline
      [:label "steps:" (str stream-index "/" stream-length)]
      [:label " -> "]
      [:label "time:"  (/ (reduce + (map :dt (:inputs stream))) 1000)]
      [:div.wave-wrapper
        [wave-widget {
                      :stream stream
                      :position position}]]]))
