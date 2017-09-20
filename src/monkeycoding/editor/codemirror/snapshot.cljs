(ns monkeycoding.editor.codemirror.snapshot
  (:require [monkeycoding.editor.codemirror.parse :refer [
                                                          js->marks
                                                          js->selection
                                                          mark->js
                                                          position->js]]))

(defn- merge-intersection [current prv]
  (let [base (select-keys prv (keys current))]
    (merge
          base
          (select-keys current (keys base)))))


(defn- take-marks [codemirror prv-marks]
  (-> codemirror
    (.getAllMarks)
    (js->marks)
    (merge-intersection prv-marks)))


(defn- take-selection [codemirror]
  (-> codemirror
    (.listSelections)
    (aget 0)
    (js->selection)))


(defn- apply-marks! [codemirror marks]
  (doseq [[_ mark] marks]
      (let [[from to options] (mark->js mark)]
        (.markText codemirror from to options))))


(defn- apply-selection! [codemirror {:keys [from to]}]
  (.setSelection codemirror (position->js from) (position->js to)))


(defn take-snapshot [codemirror prv-marks-data]
 {
    :text (.getValue codemirror)
    :marks (take-marks codemirror prv-marks-data)
    :selection (take-selection codemirror)})


(defn- apply-snapshot! [codemirror {:keys [text selection marks]}]
  (doto codemirror
    (.setValue text)
    (apply-selection! selection)))