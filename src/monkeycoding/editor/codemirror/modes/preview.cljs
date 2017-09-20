(ns monkeycoding.editor.codemirror.modes.preview
  (:require
    [monkeycoding.editor.codemirror.snapshot   :as snapshot]))


;; lifesycle
(defn sync-with-props! [this cm  props]
  (do
    (set! (.. cm -options -readOnly) true)
    (snapshot/apply-snapshot! cm (select-keys props [:selection :text :marks])))

  this)


(defn enter! [this cm props]
  (sync-with-props! this cm props))


(defn exit! [_ cm]
  (set! (.. cm -options -readOnly) false))
