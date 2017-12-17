(ns monkeycoding.widgets.scroll
    (:require
      [reagent.core :as r :refer [atom with-let]]
      [monkeycoding.widgets.util :refer [as-component]]))



(defn scroll-panel [_]
  (as-component {
                  :on-mount #(new js/SimpleBar (r/dom-node %))
                  :render (fn [props & children]
                            (if (map? props)
                              (into [] (concat [:div.scroll-panel props] children))
                              (into [] (concat [:div.scroll-panel] [props] children))))}))


(defn drag-scroll-panel [{ :keys [scrollable]}]
  (let [
        last-width (r/atom nil)
        sync! #(when (= @last-width (.-scrollWidth %)
                       (.reset js/dragscroll)
                       (reset! last-width (.-scrollWidth %))))]
    (as-component {
                    :on-mount #(.reset js/dragscroll)
                    :on-props #(do
                                 (when-not (:scrollable %1) (set! (.-scrollLeft (r/dom-node %2)) 0))
                                 (sync! (r/dom-node %2)))

                    :after-render #(sync! (r/dom-node %))
                    :render (fn [{ :keys [scrollable] :as props} & children]
                              (if-not scrollable
                                (apply vector :div (dissoc props :scrollable) children)
                                (apply vector :div.dragscroll (dissoc props :scrollable) children)))})))
