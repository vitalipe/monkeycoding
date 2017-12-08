(ns monkeycoding.widgets.label
    (:require
      [reagent.core :as r :refer [atom with-let]]
      [monkeycoding.widgets.icon :refer [icon]]))




(defn editable-label [{ :keys [value on-change class on-edit]
                        :or {on-edit identity on-change identity}}]
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
                             :on-change #(do
                                          (swap! state assoc :text (.. % -target -value))
                                          (on-edit (:text @state)))
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
