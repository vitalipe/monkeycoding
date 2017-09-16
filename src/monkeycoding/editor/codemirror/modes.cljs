(ns monkeycoding.editor.codemirror.modes
  (:require [monkeycoding.editor.codemirror.recording :refer [process-input-event]]))


(defprotocol EditMode
  (sync-with-props! [this codemirror props])
  (enter!           [this codemirror props])
  (cleanup!         [this codemirror])
  (process-input    [this codemirror event]))


(defrecord RecordingMode [last
                          last-input
                          last-selection
                          last-time
                          on-input]
  EditMode
    (sync-with-props! [this _ {on-input :on-input}] (assoc this :on-input on-input))
    (enter!           [this _ props] (sync-with-props! this _ props))
    (cleanup!         [this _] this)
    (process-input    [this codemirror event] (process-input-event this codemirror event)))


(defrecord HighlightingMode []

  EditMode
    (sync-with-props! [this codemirror props] this)
    (enter!           [this codemirror props] this)
    (cleanup!         [this codemirror] this)
    (process-input    [this codemirror event] this))


(defrecord ViewOnlyMode []

  EditMode
    (sync-with-props! [this codemirror props] this)
    (enter!           [this codemirror props] this)
    (cleanup!         [this codemirror] this)
    (process-input    [this codemirror event] this))


(defrecord UninitializedMode []

  EditMode
    (sync-with-props! [this _ _] this)
    (enter!           [this codemirror props] this)
    (cleanup!         [this _] this)
    (process-input    [this _ _] this))



(def all {
            :recording (RecordingMode. nil nil nil nil nil)
            :highlighting (HighlightingMode.)
            :view-only (ViewOnlyMode.)
            :uninitialized (UninitializedMode.)})
