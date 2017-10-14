(ns monkeycoding.editor.timeline.widgets
    (:require
      [reagent.core :as r]
      [monkeycoding.editor.common :refer [as-component]]))



(defn- event->progress-width [event]
  (let [target (.. event -target)]
    (if (.contains (.. target -classList) "progress-bar")
      (.. target -parentElement -clientWidth)
      (.. target -clientWidth))))


(defn timeline-pin []
  [:div.timeline-pin])


(defn timeline-progress [{:keys [
                                 open
                                 progress
                                 on-seek]}]
  [:div.progress-panel {
                        :class (when-not open "hidden")
                        :on-click #(on-seek (event->progress-width %) (.-clientX %))}

      (let [[total complete] (cond
                              (vector? progress) (map #(str % "px") progress)
                              (number? progress) ["100%" (str progress "%")])]

        [:div.progress.timeline-progress {:style {:width total}}
          [:div.progress-bar {:role "progressbar" :style {:width complete}}]])])


(defn collapsible-v-panel [{show :show} child]
  [:div.collapsible-v-panel {:class (if-not show "hide" "")} child])


(defn scroll-panel []
  (as-component {
                  :on-mount #(new js/SimpleBar (r/dom-node %))
                  :render (fn [props & children]
                            (if (map? props)
                              (into [] (concat [:div.scroll-panel {:class (:class props)}] children))
                              (into [] (concat [:div.scroll-panel] [props] children))))}))
