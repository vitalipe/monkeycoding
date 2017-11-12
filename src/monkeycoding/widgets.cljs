(ns monkeycoding.widgets
    (:require
      [reagent.core :as r :refer [atom with-let cursor]]
      [monkeycoding.util  :refer [as-component]]))


;; helper functions

(defn- element-has-class? [target class]
  (.contains (.. target -target -classList) class))


;; widgets

(defn scroll-panel [initial-props]
  (as-component {
                  :on-mount #(new js/SimpleBar (r/dom-node %))
                  :render (fn [props & children]
                            (if (map? props)
                              (into [] (concat [:div.scroll-panel props] children))
                              (into [] (concat [:div.scroll-panel] [props] children))))}))


(defn icon [icon-name]
  (->> (case icon-name
          :export      "android-share-alt"
          :commit      "merge"
          :redo        "forward"
          :undo        "reply"
          :record      "record"
          :menu        "navicon"
          :play        "play"
          :pause       "ios-pause"
          :goto-next   "play.adjust-smaller"
          :goto-prv    "play.horizontal-flip.adjust-smaller"
          :goto-start  "ios-skipbackward"
          :goto-end    "ios-skipforward"
          :timeline    "ios-pulse-strong"
          :close       "close"
          :add-mark    "pricetag.adjust-smaller"
          :marks       "pricetags.adjust-smaller"
          :delta-time  "android-stopwatch"
          :baseline    "qr-scanner"
          :arrow-down  "ios-arrow-down"
          :checked     "checkmark"
          icon-name)

    (str ".icon" ".ion-")
    (str "i." "icon-" (name icon-name))
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


(defn dropdown-text-item [{:keys [text disabled on-click checked]}]
  [:button.dropdown-item {
                          :class (when disabled "disabled")
                          :on-click on-click}

                       (when checked [icon :checked])
                       text])


(defn dropdown-submenu [{text :text} & items]
  [:button.dropdown-item.dropdown-submenu
    [dropdown-text-item {:text text}]
    [:div.dropdown-menu items]])


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


(defn combo-label [{:keys [on-text-change text class]} & menu-items]
  (with-let [open (r/atom false)]
     [:div.combo-label.dropdown {:class class}
       [:span {:on-click #(swap! open not)} [icon :arrow-down]]
       [editable-label {
                         :value text
                         :on-change (or on-text-change)}]
      [:div.dropdown-overlay {:class (when-not @open "hidden") :on-click #(swap! open not)}]
      [:ul.dropdown-menu {:class (when @open "show")} menu-items]]))




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


(defn modal [{:keys [class on-close]} & content]
    [:div.modal.modal-bg {:on-click #(when (element-has-class? % "modal-bg") (on-close))}
      [:div.modal-dialog {:class class}
        (apply conj [:div.modal-content] content)]])


(defn modal-header  [& content] (apply conj [:div.modal-header] content))
(defn modal-content [& content]  (apply conj [:div.modal-body] content))
(defn modal-footer  [& content]   (apply conj [:div.modal-footer] content))


(defn- option-item [title widget]
  [:div.option-item
    [:label.title title]
    [:div.padding]
    widget])
