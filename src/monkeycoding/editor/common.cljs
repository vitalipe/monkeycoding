(ns monkeycoding.editor.common
    (:require [reagent.core :refer [create-class]]))

(def default-config {
                      :mode "javascript"
                      :theme "seti"
                      :show-line-numbers true})


(defn as-component [{:keys [on-mount
                            on-props
                            render] :or {on-mount #() on-props #()}}]
  (create-class {
                  :component-did-mount on-mount
                  :component-will-receive-props #(on-props (second %2))
                  :reagent-render render}))
