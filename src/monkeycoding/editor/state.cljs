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
                            :snapshot  stream/empty-snapshot
                            :position 0

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
  (doto editor-state
    (swap! update :recording stream/append-input event snapshot dt)
    (swap! assoc  :position (dec (count (get-in @editor-state [:recording :inputs]))))
    (swap! assoc :snapshot (stream/stream->snapshot (:recording @editor-state)))))


(defn toggle-record-highlight [event]
  (state-swap! update :recording-highlight not))


(defn record-highlight [from to]
    (doto editor-state
      (swap! update :recording stream/append-mark {:from from :to to :info (str  "dummy into for: " from to)})
      (swap! assoc :snapshot (stream/stream->snapshot (:recording @editor-state) (:position @editor-state)))
      (swap! assoc :recording-highlight false)))


(defn discard-recording []
  (state-swap! merge {
                      :recording stream/empty-stream
                      :position 0
                      :snapshot stream/empty-snapshot}))


;; playback actions
(defn start-playback []
    (state-swap! assoc :current-mode :playback-mode))


(defn toggle-playback-pause []
  (state-swap! update-in [:current-mode :paused] not))


(defn next-postition []
  (when true
    (doto editor-state
      (swap! update :position inc)
      (swap! assoc :snapshot (stream/stream->snapshot (:recording @editor-state) (:position @editor-state))))))


(defn previous-postition []
  (when true
    (doto editor-state
      (swap! update :position dec)
      (swap! assoc :snapshot (stream/stream->snapshot (:recording @editor-state) (:position @editor-state))))))



(defn stop-playback []
  (state-swap! assoc :current-mode :default-mode))
