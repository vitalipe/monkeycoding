(ns monkeycoding.editor.codemirror.modes.preview
  (:require
    [monkeycoding.editor.codemirror.snapshot   :as snapshot]
    [monkeycoding.editor.codemirror.parse      :as parse]))


(defn- xy->position [cm xy]
  (when xy
    (->> xy
      (.coordsChar cm)
      (parse/js->position))))


(defn- event->position [cm dom-event]
  (let [extract #(js-obj "top" (.-pageY %) "left" (.-pageX %))]
    (->> (cond
          (= "touchstart" (.-type dom-event)) (extract (aget (.-touches dom-event) 0))
          (= "mousemove"  (.-type dom-event)) (extract dom-event))

      (xy->position cm))))


(defn- sort-marks-by-position [marks]
  (->> (vals marks)
    (sort-by (comp (juxt :line :ch) :from))))


(defn- before? [{before-ch :ch before-line :line} {after-ch :ch after-line :line}]
  (cond
    (> after-line before-line) true
    (> before-line after-line) false
    :otherwise                 (>= after-ch before-ch)))


(defn- inside-mark? [{:keys [from to] :as mark}, pos]
  (when-not (contains? #{pos mark} nil)
    (and
        (before? from pos)
        (before? pos to))))


(defn- after-mark? [{:keys [from]}, pos]
  (when-not (contains? #{pos from} nil)
    (before? from pos)))


;; liner time should be fine here
(defn- find-mark-at [sorted-marks pos]
  (when-not (empty? sorted-marks)
    (loop [[mark & remaining] sorted-marks result nil]
      (cond
        (inside-mark? mark pos) (recur remaining  mark)
        (after-mark?  mark pos) (recur remaining  result)
        :otherwise result))))


;;(defn- create-mark-element [text]
;;  (.createElement js/document "div"))
    ;;(.-innerHTML text)))


(defn- activate-line [cm line prv]
  (doto cm
    (.removeLineClass prv "")
    (.addLineClass line "" "CodeMirror-activeline-background")))


(defn- activate-mark [cm sorted-marks position]
  (.log js/console position (find-mark-at sorted-marks position)))



;; lifesycle
(defn sync-with-props! [this cm  props]
  (do
    (set! (.. cm -options -readOnly) true)
    (snapshot/apply-snapshot! cm (select-keys props [:selection :text :marks])))

  (assoc this :marks (sort-marks-by-position (:marks props))))


(defn enter! [this cm props]
  (sync-with-props! this cm props))


(defn exit! [_ cm]
  (set! (.. cm -options -readOnly) false))


(defn process-dom-event [{:keys [previous-selected-line] :as state} cm event]
  (if-let [{:keys [line] :as pos} (event->position cm event)]
    (do
      (activate-line cm line previous-selected-line)
      (activate-mark cm (:marks state) pos)
      (assoc state :previous-selected-line line))

    ;; otherwise
    state))
