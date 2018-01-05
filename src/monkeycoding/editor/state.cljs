(ns monkeycoding.editor.state
    (:require
      [reagent.core :as r]
      [monkeycoding.editor.stream :as stream]
      [monkeycoding.editor.undo   :as undo]))



(defonce editor-state (r/atom {

                                :title "new project"
                                :config {
                                          :language "javascript"
                                          :theme "seti"
                                          :show-line-numbers true
                                          :playback-speed 1}

                                :recording stream/empty-stream
                                :position -1

                                :current-mode :preview-mode}))


;; recording actions
(defn toggle-recording []
  (if-not (= (:current-mode @editor-state) :recording-mode)
      (doto editor-state
        (swap! assoc :position (dec (count (get-in @editor-state [:recording :inputs]))))
        (swap! assoc :current-mode :recording-mode))
    (swap! editor-state assoc :current-mode :preview-mode)))


(defn finish-recording []
  (swap! editor-state assoc :current-mode :preview-mode))


(defn record-input [event snapshot dt]
  (doto editor-state
    (swap! update :recording stream/append-input event snapshot dt)
    (swap! assoc  :position (dec (count (get-in @editor-state [:recording :inputs]))))))


(defn toggle-record-highlight [event]
  (if (:current-mode :highlighting-mode)
    (swap! editor-state assoc :current-mode :recording-mode)
    (swap! editor-state assoc :current-mode :highlighting-mode)))


(defn record-highlight [from to data]
    (doto editor-state
      (swap! update :recording stream/attach-mark-on from to data)
      (swap! assoc  :current-mode :recording-mode)))


;; playback actions
(defn start-playback []
    (swap! editor-state assoc :current-mode :playback-mode))


(defn toggle-playback-pause []
  (swap! editor-state update-in [:current-mode :paused] not))

(defn update-player-progress [position]
  (swap! editor-state assoc :position position))

(defn goto-postition [position]
  (when-not (= position (:position @editor-state))
    (doto editor-state
      (swap! assoc :current-mode :preview-mode)
      (swap! assoc :position position))))


(defn next-postition []
  (goto-postition  (inc (:position @editor-state))))


(defn previous-postition []
  (goto-postition  (dec (:position @editor-state))))


(defn stop-playback []
  (swap! editor-state assoc :current-mode :preview-mode))


(defn rename [name]
  (swap! editor-state assoc :title name))

(defn reset [name]
  (swap! editor-state assoc
                            :recording stream/empty-stream
                            :position -1))

(defn squash []
  (let [{:keys [recording position]} @editor-state]
    (doto editor-state
      (swap! assoc :recording (stream/squash recording position))
      (swap! assoc :position -1))))



(undo/init! editor-state)
