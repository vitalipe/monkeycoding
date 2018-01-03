(ns monkeycoding.editor.codemirror.preview
  (:require
    [monkeycoding.editor.codemirror.textarea :refer [text-area]]))



(defn code-preview [{:keys [code language]}]
  [text-area {
              :text code
              :class "code-preview"
              :config {
                       :language language
                       :read-only true}}])


(defn html-preview [html]
  [code-preview {:code html :language "htmlmixed"}])


(defn js-preview [code]
  [code-preview {:code code :language "javascript"}])
