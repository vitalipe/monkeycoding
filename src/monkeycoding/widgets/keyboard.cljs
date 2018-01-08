(ns monkeycoding.widgets.keyboard
    (:require
      [reagent.core :as r :refer [atom]]
      [monkeycoding.widgets.util :refer [as-component]]))


(def keyboard-lock (r/atom 0))

(defn keyboard-shortcuts-block []
  (as-component {
                 :on-mount (fn [_]  (swap! keyboard-lock inc))
                 :on-unmount (fn [_] (swap! keyboard-lock dec))
                 :render (fn [] [:div.keyboard-shortcuts-block])}))


(defn keyboard-shortcuts [& key-list]
  (let [
        keys (reduce-kv #(assoc %1 (set %2) %3) {} (apply hash-map key-list))
        event->keys #(-> #{
                            (when (.-ctrlKey %) :ctrl)
                            (when (.-shiftKey %) :shift)
                            (keyword (.-key %))}
                        (disj nil))

        handler (fn [evt]
                  (when (zero? @keyboard-lock)
                    (when-let [callback (get keys (event->keys evt))] (callback))))]

      (as-component {
                      :on-mount (fn [_]  (.addEventListener js/window "keydown" handler))
                      :on-unmount (fn [_] (.removeEventListener js/window "keydown" handler))
                      :render (fn [] [:div.keyboard-shortcuts])})))
