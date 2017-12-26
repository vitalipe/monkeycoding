 (ns monkeycoding.widgets.util
    (:require [reagent.core :refer [create-class]]))


(defn target-of-class? [class evt]
  (.contains (.. evt -target -classList) class))


(defn as-component [{:keys [on-mount
                            after-render
                            on-props
                            on-unmount
                            render] :or {after-render #() on-unmount #() on-mount #() on-props #()}}]
  (create-class {
                  :component-did-mount on-mount
                  :component-did-update after-render
                  :component-will-receive-props #(on-props (second %2) %1)
                  :component-will-unmount on-unmount
                  :reagent-render render}))
