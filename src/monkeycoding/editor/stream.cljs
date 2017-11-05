(ns monkeycoding.editor.stream
  (:require
      [clojure.set :refer [difference]]))



(def empty-snapshot {
                      :text ""
                      :selection {:from {:line 0 :ch 0} :to {:line 0 :ch 0}}
                      :marks {}})


(def empty-stream {
                    :inputs []
                    :marks {}
                    :initial empty-snapshot

                    :next-mark-id 0})




;; helpers
(defn- sync-marks-with-snapshot [meta {marks :marks} index]
  (let [
        prv-live-marks (into #{} (map :id (remove :remove (vals meta))))
        marks-to-kill (difference prv-live-marks (into #{} (keys marks)))]

      (->> marks-to-kill
        (map meta)
        (map #(hash-map (:id %) (assoc % :remove index)))
        (apply merge meta))))


(defn- group-marks-by-insert [marks offset]
  (->> (vals marks)
    (map #(update % :insert + offset))
    (group-by :insert)))


(defn- insert-marks-into-input-stream [inputs marks]
  (let [
        grouped-marks (into []  (group-marks-by-insert marks -1))
        strip-mark #(select-keys % [:id :from :to])
        insert (fn [stream i m] (update stream i assoc :marks (map strip-mark m)))]

    (loop [[[i, m] & rest] grouped-marks, stream (into [] inputs)]
          (if i
            (recur rest (insert stream i m))
            stream))))


(defn- snapshot-with-marks-info [snapshot marks-info]
  (if-not snapshot
    empty-snapshot

    (assoc snapshot :marks
      (->> (vals (:marks snapshot))
        (map #(assoc % :info (get-in marks-info [(:id %) :info])))
        (map #(hash-map (:id %) %))
        (apply merge)))))


;; stream
(defn stream->playback [{:keys [marks inputs initial]}]
  (let [raw-inputs (map #(dissoc % :snapshot) inputs)]
    (clj->js {
               "initial" initial
               "inputs"  (insert-marks-into-input-stream raw-inputs marks)
               "marksInfo" (reduce-kv #(assoc %1 %2 (:info %3)) {} marks)})))


(defn stream->playback-snapshot [{:keys [initial inputs marks]} index]
  (let [raw-inputs (map #(dissoc % :snapshot) inputs)]
    (clj->js {
               "initial" (if (= -1 index) initial (get-in inputs [index :snapshot]))
               "inputs"  []
               "marksInfo" (reduce-kv #(assoc %1 %2 (:info %3)) {} marks)})))


(defn stream->snapshot
  ([{inputs :inputs :as snapshot}]    (stream->snapshot snapshot (dec (count inputs))))
  ([{:keys [initial marks inputs]} i] (snapshot-with-marks-info (get (nth inputs i nil) :snapshot initial) marks)))


(defn append-mark [stream  {:keys [info to from]}]
  (let [
        id (inc (:next-mark-id stream))
        index (dec (count (:inputs stream)))]
    (-> stream
      (assoc :next-mark-id id)
      (update-in [:inputs index :snapshot :marks] assoc id {
                                                            :id id
                                                            :from from
                                                            :to to})
      (update :marks assoc id {
                                :id id
                                :from from
                                :to to
                                :insert index
                                :remove nil
                                :info info}))))


(defn append-input [stream step snapshot dt]
  (-> stream
    (update :inputs conj (merge step {:snapshot snapshot, :dt dt}))
    (update :marks sync-marks-with-snapshot snapshot (count (:inputs stream)))))


(defn squash [{:keys [inputs marks]} index]
  (let [
        [_ rest-inputs] (split-at (inc index) inputs)
        initial (get-in inputs [index :snapshot])]

    {:initial initial
     :inputs (into [] rest-inputs)
     :marks marks}))


;; inputs
(defn- create-selection-input [from to]
  {
    :type :selection
    :from from
    :to to})


(defn- create-text-input [insert remove position]
  {
    :type :input
    :insert insert
    :remove remove
    :position position})



(defn- create-cursor-input [position]
  {
    :type :cursor
    :position position})
