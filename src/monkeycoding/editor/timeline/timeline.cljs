(ns monkeycoding.editor.timeline
    (:require
      [reagent.core :as r]
      [monkeycoding.editor.common            :refer [as-component]]
      [monkeycoding.editor.timeline.progress :refer [wave-progress time-progress]]
      [monkeycoding.editor.timeline.wave     :as wave]))



;; helper widgets
(defn collapsible-v-panel [show child]
  [:div.collapsible-v-panel {:class (if-not show "hide" "")} child])


(defn scroll-panel []
  (as-component {
                  :on-mount #(new js/SimpleBar (r/dom-node %))
                  :render (fn [props & children]
                            (if (map? props)
                              (into [] (concat [:div.scroll-panel {:class (:class props)}] children))
                              (into [] (concat [:div.scroll-panel] [props] children))))}))


(defn wave-canvas [{:keys [
                            position
                            stream
                            on-seek-px]}]

  (let [
        the-wave (r/atom nil)
        prv-stream (r/atom nil)]

    (as-component {
                      :on-mount (fn [this]
                                  (reset! the-wave (new wave/Wave
                                                    (r/dom-node this)
                                                    (js-obj
                                                            "MsToPxRatio" 10
                                                            "onSeek" on-seek-px)))
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



(defn timeline-panel [{:keys [
                                stream
                                open
                                on-seek
                                position]}]

    (let []
      [:div.timeline-container
        [time-progress {
                        :inputs (:inputs stream)
                        :open (not open)
                        :position position
                        :on-seek on-seek}]

        [collapsible-v-panel open
          [scroll-panel
              [wave-progress {:open open :inputs (:inputs stream) :on-seek on-seek}]
              [wave-canvas {
                            :on-seek #()
                            :stream stream
                            :position position}]]]]))
