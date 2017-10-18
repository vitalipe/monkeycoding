(ns monkeycoding.editor.codemirror.modes.highlighting
  (:require
    [monkeycoding.editor.codemirror.snapshot   :as snapshot]))


(defn- set-marking-style [cm]
  (-> cm
      (.getWrapperElement)
      (.-classList)
      (.add "editor-mode-highlighting")))


(defn- clear-marking-style [cm]
  (-> cm
      (.getWrapperElement)
      (.-classList)
      (.remove "editor-mode-highlighting")))


(defn- non-empty-selection? [{:keys [type from to]}]
  (and
    (= type :selection)
    (not= from to)))

(defn- commit-mark [{:keys [selection callback] :as state}]
  (when (non-empty-selection? selection)
    (let [{:keys [from to]} selection]
      (cond
        (> (:line from) (:line to)) (callback to from)
        (> (:ch from) (:ch to))     (callback to from)
        :otherwise                  (callback from to))))
  (assoc state :selection {}))


;; lifesycle
(defn sync-with-props! [state cm {on-highlight :on-highlight :or {on-highlight identity}}]
  (assoc state :callback on-highlight))


(defn enter! [state cm props]
  (snapshot/apply-snapshot! cm props)
  (let [snapshot (snapshot/take-snapshot cm (:marks props))]
    (doto cm
      (->
        (.. -options -readOnly)
        (set! true))

      (.setSelection #js {"ch" 0 "line" 0} #js {"ch" 0 "line" 0})
      (set-marking-style))

    (-> state
      (assoc :snapshot snapshot)
      (sync-with-props! cm props))))


(defn exit! [{snapshot :snapshot} cm]
  (do
    (set! (.. cm -options -readOnly) false)
    (clear-marking-style cm))
  (snapshot/apply-snapshot! cm snapshot))


(defn process-dom-event [state cm dom-event]
    (case (.-type dom-event)
         ("mouseleave" "mousedown" "touchstart") (assoc state :selection {})
         ("mouseup" "touchend") (commit-mark state)
      state))

(defn process-input-event [state cm event]
  (merge state
    (when (non-empty-selection? event) {:selection event})))
