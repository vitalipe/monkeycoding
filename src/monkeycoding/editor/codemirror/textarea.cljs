(ns monkeycoding.editor.codemirror.textarea
  (:require
    [reagent.core :as r]
    [monkeycoding.widgets.util      :refer [as-component]]
    [monkeycoding.editor.codemirror :refer [create-codemirror!]]))



(defn text-area [{:keys [
                         text
                         class
                         config
                         on-change] :or {on-change #()}}]
  (let [
        cm (r/atom nil)
        config (->> config (merge {
                                   :theme "seti"
                                   :read-only false
                                   :show-line-numbers false}))]
    (as-component {
                    :on-mount (fn [this]
                                (reset! cm (create-codemirror! (r/dom-node this) config))
                                (doto @cm
                                  (.setValue text)
                                  (.on "change"#(on-change (.getValue %)))))

                    :on-props  (fn [{:keys [text language]}]
                                 (when (not= text (.getValue @cm))
                                   (.setValue @cm text)))

                    :render   (fn []
                                [:div.text-area {:class class}])})))


(defn markdown-text-area [{:keys [text on-change class]}]
  (text-area {
              :text text
              :on-change on-change
              :class class
              :config {:language "markdown"}}))


(defn json-text-area [{:keys [text on-change class]}]
  (text-area {
              :text text
              :on-change on-change
              :class class
              :config {:language "javascript"}}))
