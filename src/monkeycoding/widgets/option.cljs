(ns monkeycoding.widgets.option
  (:require
    [monkeycoding.widgets.label :refer [editable-label]]))

(defn option-item [title widget]
  [:div.option-item
    [:label.title title]
    [:div.padding]
    widget])



(defn label-option [{:keys [title value on-change on-edit]
                     :or {on-edit identity on-change identity}}]
  [option-item title
    [editable-label {:value value :on-edit on-edit :on-change on-change}]])


(defn boolean-option [{:keys [title value on-change]}]
  [option-item
    title
    [:div.boolean-switch
      [:label.bool-false {
                          :on-click #(on-change false)
                          :class (when-not value "active")}
        "false"]
      [:label.spacer "|"]
      [:label.bool-true {
                          :on-click #(on-change true)
                          :class (when value "active")}
        "true"]]])
