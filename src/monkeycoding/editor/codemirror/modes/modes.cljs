(ns monkeycoding.editor.codemirror.modes
  (:require
            [monkeycoding.editor.codemirror.modes.highlighting :as highlighting]))



(defprotocol EditMode
  (sync-with-props! [this codemirror props])
  (enter!           [this codemirror props])
  (exit!            [this codemirror])

  (process-input-event  [this codemirror event])
  (process-dom-event    [this codemirror event]))


(defrecord HighlightingMode [
                              selection
                              callback
                              snapshot]
  EditMode
    (sync-with-props! [this codemirror props] (highlighting/sync-with-props! this codemirror props))
    (enter!           [this codemirror props] (highlighting/enter! this codemirror props))
    (exit!            [this codemirror]       (highlighting/exit! this codemirror))

    (process-input-event [this cm event] (highlighting/process-input-event this cm event))
    (process-dom-event  [this cm event] (highlighting/process-dom-event this cm event)))


(defrecord UninitializedMode []
  EditMode
    (sync-with-props! [this _ _] this)
    (enter!           [this _ _] this)
    (exit!            [this _]   this)

    (process-input-event [this _ _] this)
    (process-dom-event   [this _ _] this))



(def all {
            :highlighting (HighlightingMode. {} identity nil)
            :uninitialized (UninitializedMode.)})
