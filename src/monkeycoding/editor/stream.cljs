(ns monkeycoding.editor.stream
  (:require
      [clojure.set :refer [difference]]))


;; a single frame slice, used as an initial entery point,
;; and as an optimization for fast, seeks
(def empty-snapshot {
                      :text ""
                      :selection {:from {:line 0 :ch 0} :to {:line 0 :ch 0}}
                      :marks []})

;; a mark is a range that points to a mark-data node,
;; there potentially many marks that point to the same highlight
(def empty-mark {
                 :data-id 0
                 :from {:ch 0 :line 0}
                 :to   {:ch 0 :line 0}})

;; provides metadata for marks
(def empty-mark-data {
                      :id nil
                      :info "add description..."
                      :class-names []

                      ;; pre calcaulated stream positions, for faster lookups
                      :inserted-at nil})
(def empty-stream {
                    :inputs []
                    :marks-data {} ;; indexed my id
                    :initial empty-snapshot

                    ;; should increment for every new data node
                    :next-mark-data-id 1})


;; inputs
(defn create-selection-input [from to]
  {
    :type :selection
    :from from
    :to to})


(defn create-text-input [insert remove position]
  {
    :type :input
    :insert insert
    :remove remove
    :position position})



(defn create-cursor-input [position]
  {
    :type :cursor
    :position position})


(defn create-mark-data [id]
  (assoc empty-mark-data
          :id id
          :class-names [
                        "highliting-mark"
                        (str "highliting-mark-id-" id)]))


(defn- inputs->compact-inputs [inputs]
  (if (empty? inputs)
    []
    (loop [[input & rest] inputs,  inserted #{} stream []]
      (let [
            all-marks (get-in input [:snapshot :marks])
            new-marks (filter #(contains? inserted (:data-id %)) all-marks)
            compressed-input (-> input
                               (dissoc :snapshot)
                               (assoc :marks new-marks))]
           (if (empty? rest)
             (conj stream compressed-input)
             (recur
                rest
                (apply conj inserted (map :data-id new-marks))
                (conj stream compressed-input)))))))


;; stream
(defn stream->playback [{:keys [marks-data inputs initial]}]
  (clj->js {
             "initial"   initial
             "inputs"    (inputs->compact-inputs inputs)
             "marksData" (reduce-kv #(assoc %1 %2 (dissoc %3 :inserted-at)) {} marks-data)}))


(defn stream->playback-snapshot [{:keys [initial inputs marks-data]} index]
  (let [raw-inputs (map #(dissoc % :snapshot) inputs)]
    (clj->js {
               "initial" (if (= -1 index) initial (get-in inputs [index :snapshot]))
               "inputs"  []
               "marksData" (reduce-kv #(assoc %1 %2 (dissoc %3 :inserted-at)) {} marks-data)})))


(defn stream->snapshot
  ([{inputs :inputs :as snapshot}] (stream->snapshot snapshot (dec (count inputs))))
  ([{:keys [initial inputs]} i]    (get (nth inputs i nil) :snapshot initial)))


(defn attach-mark-on [stream from to data]
  (let [
        index (dec (count (:inputs stream)))
        id     (:next-mark-data-id stream)
        mark   (assoc data
                          :id id
                          :inserted-at index
                          :class-names ["highliting-mark" (str "highliting-mark-id-" id)])]
    (-> stream
      (update :next-mark-data-id inc)
      (update-in [:inputs index :snapshot :marks] conj {:from from :to to :data-id id})
      (assoc-in [:marks-data id] mark))))


(defn append-input [stream step snapshot dt]
  (update stream :inputs conj (merge step {:snapshot snapshot, :dt dt})))


(defn squash [{:keys [inputs marks-data]} index]
  (let [
        [_ rest-inputs] (split-at (inc index) inputs)
        initial (get-in inputs [index :snapshot])]

    {:initial initial
     :inputs (into [] rest-inputs)
     :marks-data marks-data})) ;; FIXME :rebalace
