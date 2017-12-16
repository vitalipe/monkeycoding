(ns monkeycoding.editor.timeline.widgets
    (:require
      [reagent.core :as r]
      [monkeycoding.widgets.util :refer [as-component]]))




(defn- event->progress-width [event]
  (let [target (.. event -target)]
    (if (.contains (.. target -classList) "progress-bar")
      (.. target -parentElement -clientWidth)
      (.. target -clientWidth))))


(defn timeline-pin [{:keys [position on-click count]}]
  [:div.timeline-pin {
                      :class (when (= position "100%") "last")
                      :on-click (fn [evt]
                                  (.stopPropagation evt)
                                  (on-click))

                      :style {:left position}}
                (or count "?")])


(defn timeline-progress [{:keys [
                                 open
                                 progress
                                 on-seek]} content]
  (let [[total complete] (cond
                          (vector? progress) (map #(str % "px") progress)
                          (number? progress) ["100%" (str progress "%")])]

    [:div.progress-panel {
                          :style {:width total}
                          :class (when-not open "hidden")
                          ;; looks like that shitty scroller widget breaks ".pageX"
                          ;; so we need to use the non standard "offsetX", should be fine unless you use IE7 :P
                          :on-click #(on-seek (event->progress-width %) (.. % -nativeEvent -offsetX))}
        content
        [:div.progress.timeline-progress {:style {:width total}}
          [:div.progress-bar {:role "progressbar" :style {:width complete}}]]]))



(defn collapsible-v-panel [{:keys [show style]} child]
  [:div.collapsible-v-panel {:style style :class (if-not show "hide" "")} child])
