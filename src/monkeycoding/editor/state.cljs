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

                            :text ""
                            :current-mode :default-mode}))



(def state-swap! (partial swap! editor-state))

;; recording actions
(defn start-recording []
  (state-swap! assoc :current-mode :recording-mode))

(defn finish-recording []
  (do
    (state-swap! assoc :text (:snapshot (last (get-in @editor-state [:recording :inputs]))))
    (state-swap! assoc :current-mode :default-mode)))

(defn record-input [event]
  (state-swap! update-in [:recording :inputs] conj event))

(defn discard-recording []
  (state-swap! merge {
                      :recording stream/empty-stream
                      :text ""}))
;; playback actions
(defn start-playback []
    (state-swap! assoc :current-mode :playback-mode))


(defn toggle-playback-pause []
  (state-swap! update-in [:current-mode :paused] not))


(defn stop-playback []
  (state-swap! assoc :current-mode :default-mode))
