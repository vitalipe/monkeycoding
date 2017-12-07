(ns monkeycoding.widgets.dropdown
    (:require
      [monkeycoding.widgets.icon :refer [icon]]))



(defn dropdown-text-item [{:keys [text disabled on-click checked]}]
  [:button.dropdown-item {
                          :class (when disabled "disabled")
                          :on-click on-click}

                       [icon (if checked :checked :transparent)]
                       text])


(defn dropdown-submenu [{text :text} & items]
  [:button.dropdown-item.dropdown-submenu
    [dropdown-text-item {:text text}]
    [:div.dropdown-menu items]])
