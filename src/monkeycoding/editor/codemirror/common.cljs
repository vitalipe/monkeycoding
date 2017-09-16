(ns monkeycoding.editor.codemirror.common
    (:require [reagent.core :refer [create-class]]))

;; TODO: this should later move to app config
(def default-config {
                      :mode "javascript"
                      :theme "twilight"
                      :lineNumbers true})


;; TODO: this should later move to utils or something
(defn as-component [spec]
  (create-class {
                  :component-did-mount (:on-mount spec)
                  :component-will-receive-props #((:on-props spec) (second %2))
                  :reagent-render (:render spec)}))
