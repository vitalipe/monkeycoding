(ns monkeycoding.editor.stream
  (:require
      [clojure.set :refer [difference]]))


(def empty-stream {
                    :inputs []
                    :marks {}

                    :next-mark-id 0})



(def empty-snapshot {
                      :text ""
                      :selection {:from {:line 0 :ch 0} :to {:line 0 :ch 0}}
                      :marks {}})


;; helpers
(defn- sync-marks-with-snapshot [meta {marks :marks} index]
  (let [
        prv-live-marks (into #{} (map :id (remove :remove (vals meta))))
        marks-to-kill (difference prv-live-marks (into #{} (keys marks)))]

      (->> marks-to-kill
        (map meta)
        (map #(hash-map (:id %) (assoc % :remove index)))
        (apply merge meta))))



;; stream
(defn stream->playback [{marks :marks [initial inputs] :inputs}]
  {
   :initial (:snapshot initial)
   :inputs (into [] (map #(dissoc % :snapshot) inputs))
   :marks (sort-by :insert (map #(select-keys (% 1) [:id :info :insert :from :to]) marks))})


(defn stream->snapshot [{inputs :inputs}]
  (if-let [snapshot (:snapshot (last inputs))]
      snapshot
      empty-snapshot))


(defn append-mark [stream  {:keys [info to from]}]
  (let [
        id (inc (:next-mark-id stream))
        index (dec (count (:inputs stream)))]
    (-> stream
      (assoc :next-mark-id id)
      (update-in [:inputs index :snapshot :marks] assoc id {
                                                            :id id
                                                            :from from
                                                            :to to
                                                            :info info})
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
