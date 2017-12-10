(ns monkeycoding.widgets.scroll
    (:require
      [reagent.core :as r :refer [atom with-let]]
      [monkeycoding.widgets.util :refer [as-component]]))




;; widgets

(defn scroll-panel [_]
  (as-component {
                  :on-mount #(new js/SimpleBar (r/dom-node %))
                  :render (fn [props & children]
                            (if (map? props)
                              (into [] (concat [:div.scroll-panel props] children))
                              (into [] (concat [:div.scroll-panel] [props] children))))}))
