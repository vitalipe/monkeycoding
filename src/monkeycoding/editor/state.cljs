(ns monkeycoding.editor.state
    (:require
      [reagent.core :as r]
      [monkeycoding.editor.stream :as stream]))


(def editor-state (r/atom {
                            :meta {
                                    :title "new banana"
                                    :language :javascript
                                    :theme :default}

                            :recording stream/empty-stream
                            :snapshot stream/empty-snapshot

                            :recording-highlight false
                            :current-mode :default-mode}))



(def state-swap! (partial swap! editor-state))

;; recording actions
(defn start-recording []
  (state-swap! assoc :current-mode :recording-mode))


(defn finish-recording []
  (doto editor-state
    (swap! assoc :current-mode :default-mode)
    (swap! assoc :record-input false)
    (swap! assoc :recording-highlight false)
    (swap! assoc :snapshot (stream/stream->snapshot (:recording @editor-state)))))


(defn record-input [event snapshot dt]
  (do
    (state-swap! update :recording stream/append-step event snapshot dt)
    (state-swap! assoc :snapshot (stream/stream->snapshot (:recording @editor-state)))))


(defn toggle-record-highlight [event]
  (state-swap! update :recording-highlight not))


(defn record-highlight [highlight-info]
  (do
    (state-swap! assoc :recording-highlight false)
    (.log js/console "TODO" highlight-info "mooo!")))


(defn discard-recording []
  (state-swap! merge {
                      :recording stream/empty-stream
                      :snapshot stream/empty-snapshot}))


;; playback actions
(defn start-playback []
    (state-swap! assoc :current-mode :playback-mode))


(defn toggle-playback-pause []
  (state-swap! update-in [:current-mode :paused] not))


(defn stop-playback []
  (state-swap! assoc :current-mode :default-mode))
