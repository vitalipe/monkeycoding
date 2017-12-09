 (ns monkeycoding.widgets.util
    (:require [reagent.core :refer [create-class]]))


(defn target-of-class? [evt class]
  (.contains (.. evt -target -classList) class))


(defn as-component [{:keys [on-mount
                            on-props
                            on-unmount
                            render] :or {on-unmount #() on-mount #() on-props #()}}]
  (create-class {
                  :component-did-mount on-mount
                  :component-will-receive-props #(on-props (second %2))
                  :component-will-unmount on-unmount
                  :reagent-render render}))
