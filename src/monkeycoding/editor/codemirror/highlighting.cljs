(ns monkeycoding.editor.codemirror.highlighting)


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


(defn- empty-selection? [{:keys [type head anchor]}]
  (cond
    (not= type :selection) true
    (= head anchor)        true))


(defn- commit-mark [{:keys [selection callback] :as state}]
  (when-not (empty-selection? selection)
    (callback selection))

  (assoc state :selection {}))


(defn- save-selection [state cm]
  (assoc state :original-selection (aget (.listSelections cm) 0)))


(defn- restore-selection! [selection cm]
  (.setSelection  cm (.-anchor selection) (.-head selection)))

;; mode
(defn sync-with-props! [state cm {on-highlight :on-highlight :or {on-highlight identity}}]
  (assoc state :callback on-highlight))


(defn enter! [state cm props]
  (do
    (set! (.. cm -options -readOnly) true)
    (set-marking-style cm))

  (-> state
    (save-selection cm)
    (sync-with-props! cm props)))


(defn exit! [{selection :original-selection} cm]
  (do
    (set! (.. cm -options -readOnly) false)
    (clear-marking-style cm))

  (restore-selection! selection cm))


(defn process-dom-event [state cm dom-event]
    (case (.-type dom-event)
         ("mouseleave" "mousedown" "touchstart") (assoc state :selection {})
         ("mouseup" "touchend") (commit-mark state)))


(defn process-input-event [state cm event]
  (merge state
    (when-not (empty-selection? event) {:selection event})))
