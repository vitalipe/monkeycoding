(ns monkeycoding.editor.undo
    (:require
      [reagent.core :as r]
      [monkeycoding.editor.stream :as stream]))



(def undo-state (r/atom {
                          :states  []
                          :current -1
                          :editor-ref nil}))


(defn- current-undo-state []
  (let [{:keys [current states]} @undo-state]
    (cond
      (= -1 current) stream/empty-stream
      :otherwise (get states current))))


(defn- sync-with-undo-state! []
  (let [
        editor-state (:editor-ref @undo-state)
        recording    (current-undo-state)]

    (swap! editor-state merge {
                                :recording recording
                                :position (dec (count (:inputs recording)))})))


(defn- sync-with-editor-state! [{prv :recording} {next :recording}]
  (when-not (= prv next)
    (when-not (= next (current-undo-state))
      (let [{:keys [states current]} @undo-state]
        (swap! undo-state assoc :states (conj (subvec states 0 (inc current)) next))
        (swap! undo-state update :current inc)))))


;; interface
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


(defn init! [editor-state]
  (swap! undo-state assoc :editor-ref editor-state)
  (add-watch editor-state :undo-redo (fn [_ _ prv next] (sync-with-editor-state! prv next))))
