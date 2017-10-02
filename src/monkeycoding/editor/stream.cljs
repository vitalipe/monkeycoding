(ns monkeycoding.editor.stream
  (:require
      [clojure.set :refer [difference]]))


(def empty-stream {
                    :inputs []
                    :marks-metadata {}})



(def empty-snapshot {
                      :text ""
                      :selection {:from {:line 0 :ch 0} :to {:line 0 :ch 0}}
                      :marks {}})


;; stream
(defn stream->playback [{inputs :inputs}]
  {
   :initial (:snapshot (first inputs))
   :inputs (into [] (map #(dissoc % :snapshot) (rest inputs)))})


(defn stream->snapshot [{inputs :inputs}]
  (if-let [snapshot (:snapshot (last inputs))]
      snapshot
      empty-snapshot))


(defn- sync-marks-metadata-with-snapshot  [meta {marks :marks} index]
  (let [
        prv-live-marks (into #{} (map :id (remove :remove-at (vals meta))))
        marks-to-kill (difference prv-live-marks (into #{} (keys marks)))]

      (->> marks-to-kill
        (map meta)
        (map #(hash-map (:id %) (assoc % :remove-at index)))
        (apply merge meta))))


(defn- sync-marks-metadata-with-new-step [meta {:keys [type id]} index]
  (merge meta
    (when (= type :mark)
      { id {:id id :insert-at index :remove-at nil}})))


(defn append-step [stream step snapshot dt]
  (let [stream-length (count (:inputs stream))]
    (-> stream
      (update :inputs conj (merge step {:snapshot snapshot, :dt dt}))
      (update :marks-metadata sync-marks-metadata-with-new-step step stream-length)
      (update :marks-metadata sync-marks-metadata-with-snapshot snapshot stream-length))))


;; steam steps
(def ^:private next-mark-id (atom 0))


(defn- create-mark-step [from to info]
  (let [
        id (swap! next-mark-id inc)]
        ;;snapshot (update snapshot :marks assoc id {:id id, :from from, :to to, :info info})]
    {
      :type :mark
      :id id
      :from from
      :to to
      :info info}))


(defn- create-selection-step [from to]
  {
    :type :selection
    :from from
    :to to})


(defn- create-input-step [insert remove position]
  {
    :type :input
    :insert insert
    :remove remove
    :position position})



(defn- create-cursor-step [position]
  {
    :type :cursor
    :position position})
