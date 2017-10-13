(ns monkeycoding.editor.timeline.progress
    (:require
      [reagent.core :as r]))


(defn- calc-% [total position]
  (* 100 (when-not (zero? total) (/ position total))))


(defn- percentage->position [total percentage]
  (max 0 (dec (.round js/Math (* total (/ percentage 100))))))


(defn- progress [{:keys [open progress]}]
  [:div.progress.timeline-progress {:class (when-not open "hidden")}
    [:div.progress-bar {:role "progressbar" :style {:width (str progress "%")}}]])


(defn- event->progress-width [event]
  (let [target (.. event -target)]
    (if (.contains (.. target -classList) "progress-bar")
      (.. target -parentElement -clientWidth)
      (.. target -clientWidth))))


(defn wave-progress [{:keys [open inputs position]}]
  (-> (progress {:open open :progress 50})
    (update 1 merge {:style {:width "300px"}})))


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
