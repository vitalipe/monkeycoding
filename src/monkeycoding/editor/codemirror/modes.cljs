(ns monkeycoding.editor.codemirror.modes
  (:require [monkeycoding.editor.codemirror.recording :refer [process-input-event]]
            [monkeycoding.editor.codemirror.highlighting :as highlighting]))



(defprotocol EditMode
  (sync-with-props! [this codemirror props])
  (enter!           [this codemirror props])
  (exit!            [this codemirror])

  (process-codemirror-event [this codemirror codemirror-event])
  (process-dom-event        [this codemirror dom-event]))

(defrecord RecordingMode [last
                          last-input
                          last-selection
                          last-time
                          on-input]
  EditMode
    (sync-with-props! [this _ {on-input :on-input}] (assoc this :on-input on-input))
    (enter!           [this _ props] (sync-with-props! this _ props))
    (exit!            [this _] this)

    (process-codemirror-event [this cm event] (process-input-event this cm event))
    (process-dom-event        [this _ _] this))


(defrecord HighlightingMode [
                              selection
                              callback
                              original-selection]
  EditMode
    (sync-with-props! [this codemirror props] (highlighting/sync-with-props! this codemirror props))
    (enter!           [this codemirror props] (highlighting/enter! this codemirror props))
    (exit!            [this codemirror]       (highlighting/exit! this codemirror))

    (process-codemirror-event [this cm event] (highlighting/process-input-event this cm event))
    (process-dom-event        [this cm event] (highlighting/process-dom-event this cm event)))


(defrecord ViewOnlyMode []
  EditMode
    (sync-with-props! [this cm {text :text}]
                    (do
                      (when text (.setValue cm text))
                      (set! (.. cm -options -readOnly) true)
                      this))

    (enter!           [this cm props] (sync-with-props! this cm props))
    (exit!            [this cm] (set! (.. cm -options -readOnly) false))

    (process-codemirror-event [this _ _] this)
    (process-dom-event        [this _ _] this))


(defrecord UninitializedMode []
  EditMode
    (sync-with-props! [this _ _] this)
    (enter!           [this _ _] this)
    (exit!            [this _]   this)

    (process-codemirror-event [this _ _] this)
    (process-dom-event        [this _ _] this))



(def all {
            :recording (RecordingMode. nil nil nil nil nil)
            :highlighting (HighlightingMode. {} identity nil)
            :view-only (ViewOnlyMode.)
            :uninitialized (UninitializedMode.)})
