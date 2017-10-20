(ns monkeycoding.widgets
    (:require
      [reagent.core :as r :refer [atom with-let cursor]]
      [monkeycoding.util  :refer [as-component]]))



(defn icon [name]
  (->> (case name
          :export      "android-share-alt"
          :commit      "merge"
          :redo        "forward"
          :undo        "reply"
          :record      "record"
          :menu        "navicon"
          :play        "play"
          :goto-next   "ios-fastforward"
          :goto-prv    "ios-rewind"
          :goto-start  "ios-skipbackward"
          :goto-end    "ios-skipforward"
          :timeline    "ios-pulse-strong"
          :close       "close"
          :add-mark    "ios-location"
          :delta-time  "android-stopwatch"
          name)

    (str "i" ".icon" ".ion-")
    (keyword)
    (vector)))


(defn toolbar-button
  ([icon-or-props] (if (map? icon-or-props)
                    (toolbar-button icon-or-props (:icon icon-or-props))
                    (toolbar-button {} icon-or-props)))
  ([{:keys [
            disabled  ;; disabled buttons don't trigger clicks
            on-click  ;; the normal rect handler
            selected  ;; selected buttons are styled differently
            class]}   ;; custom class names
    icon-name]        ;; icon name or an icon element
   (let [custom-classes (remove nil? [
                                      (when disabled " disabled")
                                      (when selected " selected")])]
     [:span.toolbar-button {
                            :class (apply conj class custom-classes)
                            :on-click (if disabled identity on-click)}
                      (cond
                        (keyword? icon-name) [icon icon-name]
                        (string? icon-name) [icon icon-name]
                        :otherwise icon-name)])))


(defn toolbar-spacer []
  [:span.toolbar-spacer])



(defn editable-label [{:keys [value on-change class] :or {on-change identity}}]
  (with-let [
              state (r/atom {:text value :value value})
              commit #(do
                        (swap! state assoc :value (:text @state))
                        (on-change (:text @state)))]

    (when-not (= (:value @state) value)
      (reset! state {:value value :text value}))

    [:input.editable-label {
                             :type "text"
                             :value (:text @state)
                             :on-key-down #(when (= 13 (.-keyCode %)) (.blur (.-target %)))
                             :on-blur commit
                             :on-click #(.select (.-target %))
                             :on-change #(swap! state assoc :text (.. % -target -value))
                             :class class}]))


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
