(ns monkeycoding.widgets.option
  (:require
    [monkeycoding.widgets.label    :refer [editable-label combo-label bool-select-label]]
    [monkeycoding.widgets.dropdown :refer [dropdown-text-item]]
    [monkeycoding.widgets.scroll :refer [scroll-panel]]))


(defn- option-item [title widget]
  [:div.option-item
    [:label.title title]
    [:div.padding]
    widget])


(defn dropdown-option [{:keys [title options on-select selected]}]
  (let [selected-title (:title (first (drop-while #(not= (:value %) selected) options)))]
    [option-item title
      [combo-label {:text selected-title}
       [scroll-panel {:key "option-scroll-panel"}
        (->> options
          (map-indexed
            (fn [index {:keys [value title key]}]
              [dropdown-text-item {
                                    :text title
                                    :checked (= selected value)
                                    :on-click #(on-select value)
                                    :key (or key index)}])))]]]))


(defn label-option [{:keys [title value on-change on-edit]
                     :or {on-edit identity on-change identity}}]
  [option-item title
    [editable-label {:value value :on-edit on-edit :on-change on-change}]])


(defn boolean-option [{:keys [title value on-change]}]
  [option-item title [bool-select-label {:value value :on-select on-change}]])
