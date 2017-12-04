(ns monkeycoding.widgets.option
    (:require))


(defn option-item [title widget]
  [:div.option-item
    [:label.title title]
    [:div.padding]
    widget])
