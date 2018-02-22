(ns monkeycoding.editor.codemirror.parse
  (:require
    [monkeycoding.editor.stream   :as stream :refer [
                                                      create-selection-input
                                                      create-cursor-input
                                                      create-text-input]]))
(defn str->int [text]
  (.parseInt js/window (re-find #"[\d.]+" text)))


(defn js->position [obj]
  {:line (.-line obj) :ch (.-ch obj)})


(defn position->js [{:keys [line ch]}]
  (js-obj "line" line "ch" ch))


(defn js->range [obj]
  {
    :from (js->position (.-from obj))
    :to   (js->position  (.-to obj))})


(defn js->selection [obj]
  (when obj
    {
      :from (js->position (.-anchor obj))
      :to   (js->position  (.-head obj))}))


(defn js->mark [obj]
  (-> {}
    (merge (js->range  (.find obj)))
    (assoc :data-id (str->int (re-find #"\bmonkey-mark-id-\w" (.-className obj))))))


(defn mark->js [{:keys [from to data-id]}]
  [
    (position->js from)
    (position->js to)
    (js-obj
            "className" (str "monkey-mark " "monkey-mark-id-" data-id)
            "startStyle" "highliting-mark-start")])

(defn js->marks [marks-array]
  (->> marks-array
    (js->clj)
    (map js->mark)
    (into [])))

(defn js->input [data]
  (let [
        input  (js->clj data)
        origin (get input "origin")

        cursor-event? (nil? origin)
        snapshot-event? (= origin "applying-snapshot-ignore-this-event!")
        selection-event? (contains? input "ranges")
        text-event? (contains? #{"+input" "+delete" "cut" "paste" "copy" "undo" "redo"} origin)]

      (cond
        snapshot-event?  nil
        selection-event? (create-selection-input
                            (js->position (.-anchor (first (input "ranges"))))
                            (js->position (.-head (first (input "ranges")))))

        cursor-event?  (create-cursor-input
                            (js->position data))

        text-event?    (create-text-input
                            (clojure.string/join "\n" (input "text"))
                            (count (clojure.string/join "\n" (input "removed")))
                            (js->position (.-from data))))))
