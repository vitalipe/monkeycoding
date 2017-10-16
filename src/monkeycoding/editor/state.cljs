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
                            :position 0

                            :recording-highlight false
                            :current-mode :default-mode}))


;; recording actions
(defn start-recording []
  (swap! editor-state assoc :current-mode :recording-mode))


(defn finish-recording []
  (doto editor-state
    (swap! assoc :current-mode :default-mode)
    (swap! assoc :record-input false)
    (swap! assoc :recording-highlight false)))


(defn record-input [event snapshot dt]
  (doto editor-state
    (swap! update :recording stream/append-input event snapshot dt)
    (swap! assoc  :position (dec (count (get-in @editor-state [:recording :inputs]))))))


(defn toggle-record-highlight [event]
  (swap! editor-state update :recording-highlight not))


(defn record-highlight [from to]
    (doto editor-state
      (swap! update :recording stream/append-mark {:from from :to to :info (str  "dummy into for: " from to)})
      (swap! assoc :recording-highlight false)))


;; playback actions
(defn start-playback []
    (swap! editor-state assoc :current-mode :playback-mode))


(defn toggle-playback-pause []
  (swap! editor-state update-in [:current-mode :paused] not))


(defn goto-postition [position]
  (swap! editor-state assoc :position position))


(defn next-postition []
  (goto-postition  (inc (:position @editor-state))))


(defn previous-postition []
  (goto-postition  (dec (:position @editor-state))))


(defn stop-playback []
  (swap! editor-state assoc :current-mode :default-mode))



;; undo redo
(def undo-state (r/atom {
                          :states  []
                          :current -1}))

(defn- current-undo-state []
  (let [{:keys [current states]} @undo-state]
    (cond
      (= -1 current) stream/empty-stream
      :otherwise (get states current))))


(defn- sync-with-undo-state! []
  (let [recording    (current-undo-state)]
    (swap! editor-state merge {
                                :recording recording
                                :position (dec (count (:inputs recording)))})))


(defn- sync-with-editor-state! [{prv :recording} {next :recording}]
  (when-not (= prv next)
    (when-not (= next (current-undo-state))
      (let [{:keys [states current]} @undo-state]
        (swap! undo-state assoc :states (conj (subvec states 0 (inc current)) next))
        (swap! undo-state update :current inc)))))


(add-watch editor-state :undo-redo (fn [_ _ prv next] (sync-with-editor-state! prv next)))


(defn can-undo? [] (> (:current @undo-state) -1))
(defn can-redo? [] (> (count (:states @undo-state)) (inc (:current @undo-state))))


(defn undo! []
  (when (can-undo?)
      (swap! undo-state update :current dec)
      (sync-with-undo-state!)))


(defn redo! []
  (when (can-redo?)
      (swap! undo-state update :current inc)
      (sync-with-undo-state!)))
