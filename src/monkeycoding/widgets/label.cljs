(ns monkeycoding.widgets.label
    (:require
      [reagent.core :as r :refer [atom with-let]]
      [monkeycoding.widgets.icon :refer [icon]]
      [monkeycoding.widgets.util :refer [target-of-class?]]
      [monkeycoding.widgets.scroll :refer [scroll-panel]]))


(defn- props->label-class [{:keys [selected disabled class]}]
  (let [ class-names (if (coll? class) class [class])]
    (conj class-names
      (when selected "selected")
      (when disabled "disabled"))))



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
  (with-let [state (r/atom {:open false :hover false})]
     [:div.combo-label.dropdown {:class class}
       [:span.icon-wrapper {
                            :on-mouse-enter #(swap! state assoc :hover true)
                            :on-mouse-leave #(swap! state assoc :hover false)
                            :on-click #(swap! state update :open not)}
          [:span.icon-displacement {:class (when (or (:open @state) (:hover @state)) "active")}
            [icon :arrow-down]]]
       (cond
         (nil? on-text-change) [:label.label text]
         :otherwise [editable-label {
                                     :value text
                                     :on-change on-text-change}])

      [:div.dropdown-overlay {
                              :class (when-not (:open  @state) "hidden")
                              :on-click #(swap! state update :open not)}]

      [:div.dropdown-menu {
                            :on-click #(when (and
                                                (target-of-class? % "dropdown-item")
                                                (not (target-of-class? % "disabled")))
                                          (swap! state assoc :open false))

                            :class (when (:open  @state) "show")}
          [scroll-panel
            menu-items]]]))


(defn select-label [{:keys [class on-select]} & items]
  (apply vector :div.select-label {:class class}
    (->> items
      (map (fn [{:keys [value label] :as props}]
              [:label.select-label-item {
                                          :on-click #(on-select value)
                                          :class (props->label-class props)} label]))
      (interpose [:label.spacer "|"]))))


(defn bool-select-label [{:keys [value on-select]}]
  [select-label {:class "bool-select-label" :on-select on-select}
      {:value false :label "false" :class "bool-false" :selected (false? value)}
      {:value true  :label "true"  :class "bool-true"  :selected (true?  value)}])
