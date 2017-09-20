(ns monkeycoding.editor.codemirror.modes
  (:require [monkeycoding.editor.codemirror.modes.recording :as recording]
            [monkeycoding.editor.codemirror.modes.highlighting :as highlighting]
            [monkeycoding.editor.codemirror.modes.preview :as preview]))



(defprotocol EditMode
  (sync-with-props! [this codemirror props])
  (enter!           [this codemirror props])
  (exit!            [this codemirror])

  (process-input-event  [this codemirror event])
  (process-dom-event    [this codemirror event]))


(defrecord RecordingMode [last
                          last-input
                          last-selection
                          last-time
                          marks
                          on-input]
  EditMode
    (sync-with-props! [this cm props] (recording/sync-with-props! this cm props))
    (enter!           [this cm props] (recording/enter! this cm props))
    (exit!            [this _] this)

    (process-input-event  [this cm step] (recording/process-input-event this cm step))
    (process-dom-event    [this _ _] this))


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


(defrecord PreviewMode []
  EditMode
    (sync-with-props! [this cm props] (preview/sync-with-props! this cm props))
    (enter!           [this cm props] (preview/sync-with-props! this cm props))
    (exit!            [this cm] (preview/exit! this cm))

    (process-input-event  [this _ _] this)
    (process-dom-event    [this _ _] this))


(defrecord UninitializedMode []
  EditMode
    (sync-with-props! [this _ _] this)
    (enter!           [this _ _] this)
    (exit!            [this _]   this)

    (process-input-event [this _ _] this)
    (process-dom-event   [this _ _] this))



(def all {
            :recording (RecordingMode. nil nil nil nil nil nil)
            :highlighting (HighlightingMode. {} identity nil)
            :view-only (PreviewMode.)
            :uninitialized (UninitializedMode.)})