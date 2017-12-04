(ns monkeycoding.editor.codemirror.preview
  (:require
    [reagent.core :as r]
    [monkeycoding.widgets.util      :refer [as-component]]
    [monkeycoding.editor.codemirror :refer [create-codemirror!]]))



(defn code-preview [{:keys [code language]}]
  (let [
        cm (r/atom nil)
        config {
                :theme "seti"
                :read-only true
                :show-line-numbers false
                :language language}]

    (as-component {
                    :on-mount (fn [this]
                                (reset! cm (create-codemirror! (r/dom-node this) config))
                                (.setValue @cm code))

                    :on-props  (fn [{:keys [code language]}]
                                  (doto @cm
                                    (.setValue code)))

                    :render   (fn []
                                [:div.code-preview])})))



(defn html-preview [html]
  [code-preview {:code html :language "htmlmixed"}])


(defn js-preview [code]
  [code-preview {:code code :language "javascript"}])
