(ns monkeycoding.editor.timeline
    (:require
      [reagent.core :as r]
      [monkeycoding.editor.common        :refer [as-component]]
      [monkeycoding.editor.timeline.wave :as wave]))



(defn- calc-% [total position]
  (* 100 (when-not (zero? total) (/ position total))))

(defn- percentage->position [total percentage]
  (max 0 (dec (.round js/Math (* total (/ percentage 100))))))



(defn wave-widget [{:keys [
                            position
                            stream
                            on-wave-width-change
                            on-seek]}]


  (let [
        the-wave (r/atom nil)
        prv-stream (r/atom nil)]

    (as-component {
                      :on-mount (fn [this]
                                  (reset! the-wave (new wave/Wave
                                                    (r/dom-node this)
                                                    (js-obj
                                                            "onWidthChange" on-wave-width-change
                                                            "onSeek" on-seek)))
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


(defn collapsible-v-panel [show child]
  [:div.collapsible-v-panel {:class (if-not show "hide" "")} child])


(defn progress [{:keys [open progress]}]
  [:div.progress.timeline-progress {:class (when-not open "hidden")}
    [:div.progress-bar {:role "progressbar" :style {:width (str progress "%")}}]])


(defn wave-progress [{:keys [open seek width]}]
  (-> (progress {:open open :progress (* 100 (/ seek width))})
    (update 1 merge {:style {:width width}})))


(defn- event->progress-width [event]
  (let [target (.. event -target)]
    (if (.contains (.. target -classList) "progress-bar")
      (.. target -parentElement -clientWidth)
      (.. target -clientWidth))))

(defn time-progress [{:keys [open
                             inputs
                             position
                             on-seek]}]

  (r/with-let [state (r/atom {
                                :last-size (count inputs)
                                :last-progress (calc-% (count inputs) (inc position))
                                :last-position 0})]

    (let [input-size (count inputs)]
      [:div.progress-h-pad
        {:on-click (fn [evt]
                      (let [
                            progress (calc-%  (event->progress-width evt) (.-clientX evt))
                            position (percentage->position input-size progress)]
                        (on-seek position)
                        (swap! state merge {
                                            :last-size input-size
                                            :last-progress progress
                                            :last-position position})))}

        [progress {
                    :open open
                    :progress (cond
                                  (not= input-size (:last-size @state))   (calc-% input-size (inc position))
                                  (not= position (:last-position @state)) (calc-% input-size (inc position))
                                  :otherwise                              (:last-progress @state))}]])))


(defn timeline-panel [{:keys [
                                stream
                                open
                                on-seek
                                position]}]

    (r/with-let [state (r/atom {:wave-width 0 :wave-seek 200})]
      [:div.timeline-container
        [time-progress {
                        :inputs (:inputs stream)
                        :open (not open)
                        :position position
                        :on-seek on-seek}]

        [collapsible-v-panel open
          [:div.timeline
              [:div.progress-h-pad
                [wave-progress {:open open :width (:wave-width @state) :seek (:wave-seek @state)}]]
              [wave-widget {
                            :on-wave-width-change #(swap! state assoc :wave-width %)
                            :on-seek #()
                            :stream stream
                            :position position}]]]]))
