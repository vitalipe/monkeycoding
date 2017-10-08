(ns monkeycoding.editor.common
    (:require [reagent.core :refer [create-class]]))

(def default-config {
                      :mode "javascript"
                      :theme "seti"
                      :show-line-numbers true})


(defn as-component [spec]
  (create-class {
                  :component-did-mount (:on-mount spec)
                  :component-will-receive-props #((:on-props spec) (second %2))
                  :reagent-render (:render spec)}))
