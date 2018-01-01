(ns monkeycoding.widgets.dropdown
    (:require
      [monkeycoding.widgets.icon :refer [icon]]
      [monkeycoding.widgets.util :refer [target-of-class?]]))


(defn- target-dropdown-item? [evt]
  (and
    (or
      (target-of-class? "dropdown-item-icon" evt)
      (target-of-class? "dropdown-item" evt)
      (target-of-class? "text" evt))
    (not (target-of-class? "disabled" evt))
    (not (target-of-class? "dropdown-submenu" evt))))


(defn dropdown-text-item [{:keys [text disabled on-click checked] icon-name :icon}]
  [:button.dropdown-item {
                          :class (when disabled "disabled")
                          :on-click (when-not disabled on-click)}

                       [icon {
                              :class ["dropdown-item-icon" (when disabled "disabled")]
                              :icon (if checked :checked (or icon-name :transparent))}]
                       [:label.text {:class (when disabled "disabled")} text]])


(defn dropdown-menu [{:keys [open on-item-select]} & items]
    [:div.dropdown-menu {
                         :on-click #(when (target-dropdown-item? %) (on-item-select))
                         :class (when open "show")}
          items])


(defn dropdown-submenu [{text :text icon :icon} & items]
  [:div.dropdown-item.dropdown-submenu
    [dropdown-text-item {:text text :icon icon}]
    [:div.dropdown-menu items]])
