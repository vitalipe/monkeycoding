(ns monkeycoding.editor.codemirror.snapshot
  (:require [monkeycoding.editor.codemirror.parse :refer [
                                                          js->marks
                                                          js->selection
                                                          mark->js
                                                          position->js]]))


(defn- take-marks [codemirror prv-marks]
  (-> codemirror
    (.getAllMarks)
    (js->marks)))


(defn- take-selection [codemirror]
  (-> codemirror
    (.listSelections)
    (aget 0)
    (js->selection)))


(defn- apply-marks! [codemirror marks]
  (doseq [mark marks]
      (let [[from to options] (mark->js mark)]
        (.markText codemirror from to options))))


(defn apply-selection! [codemirror {:keys [from to]}]
  (.setSelection codemirror (position->js from) (position->js to) (js-obj "origin" "snapshot!")))


(defn take-snapshot [codemirror prv-marks-data]
   {
    :text (.getValue codemirror)
    :marks (take-marks codemirror prv-marks-data)
    :selection (take-selection codemirror)})


(defn apply-snapshot! [cm {:keys [text selection marks]}]
  (.operation cm #(doto cm
                    (.setValue text)
                    (apply-selection! selection)
                    (apply-marks! marks)))
  cm)


(defn same-text-and-selection? [cm {:keys [text selection]}]
    (and
        (= text (.getValue cm))
        (= selection (take-selection cm))))
