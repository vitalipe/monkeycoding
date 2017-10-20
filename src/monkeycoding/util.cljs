(ns monkeycoding.util
    (:require [reagent.core :refer [create-class]]))


(defn as-component [{:keys [on-mount
                            on-props
                            on-unmount
                            render] :or {on-unmount #() on-mount #() on-props #()}}]
  (create-class {
                  :component-did-mount on-mount
                  :component-will-receive-props #(on-props (second %2))
                  :component-will-unmount on-unmount
                  :reagent-render render}))
