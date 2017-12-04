(ns monkeycoding.widgets.keyboard
    (:require
      [monkeycoding.widgets.util :refer [as-component]]))




(defn keyboard-shortcuts [& key-list]
  (let [
        keys (reduce-kv #(assoc %1 (set %2) %3) {} (apply hash-map key-list))
        event->keys #(-> #{
                            (when (.-ctrlKey %) :ctrl)
                            (when (.-shiftKey %) :shift)
                            (keyword (.-key %))}
                        (disj nil))

        handler (fn [evt]
                  (when-let [callback (get keys (event->keys evt))] (callback)))]

      (as-component {
                      :on-mount (fn [_]  (.addEventListener js/window "keydown" handler))
                      :on-unmount (fn [_] (.removeEventListener js/window "keydown" handler))
                      :render (fn [] [:div.keyboard-shortcuts])})))
