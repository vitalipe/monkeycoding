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


(defn- create-mark-element []
  (let [element (.createElement js/document "div")]
    (doto (.-classList element)
      (.add "editor-mark-element")
      (.add "CodeMirror-activeline-background"))

    element))


(defn- activate-line! [cm line prv]
  (when prv  (.removeLineClass cm prv "" "CodeMirror-activeline-background"))
  (when line (.addLineClass cm line "" "CodeMirror-activeline-background"))
  line)

(defn- activate-mark! [cm line mark widget info-dom]
  (when widget
    (.clear widget))

  (when mark
    (set! (.-innerHTML info-dom) (:info mark))
    (let [widget (.addLineWidget cm  line info-dom #js{"above" true})]
      (set! (.. info-dom -parentElement -parentElement -style -position) "")
      widget)))


;; lifesycle
(defn sync-with-props! [this cm  props]
  (do
    (set! (.. cm -options -readOnly) true)
    (snapshot/apply-snapshot! cm (select-keys props [:selection :text :marks])))

  (assoc this :marks (sort-marks-by-position (:marks props))))


(defn enter! [state cm props]
  (-> state
    (sync-with-props! cm props)
    (assoc :info-dom (create-mark-element))))


(defn exit! [_ cm]
  (set! (.. cm -options -readOnly) false))


(defn process-dom-event [{:keys [
                                  prv-line
                                  prv-mark
                                  prv-widget
                                  marks
                                  info-dom] :as state} cm event]

  (let [
        {:keys [line] :as pos} (event->position cm event)
        mark (find-mark-at marks pos)]

    (merge state
        {
          :prv-line (activate-line! cm line prv-line)
          :prv-mark mark
          :prv-widget (activate-mark! cm line mark prv-widget info-dom)})))
