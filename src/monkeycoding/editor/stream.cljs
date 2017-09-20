(ns monkeycoding.editor.stream)


(def empty-stream [])

(def empty-snapshot {
                      :text ""
                      :selection {:from {:line 0 :ch 0} :to {:line 0 :ch 0}}
                      :marks {}})


;; stream
(defn stream->playback [stream]
  {
   :initial (:snapshot (first stream))
   :inputs (into [] (map #(dissoc % :snapshot) (rest stream)))})


(defn stream->snapshot [stream]
  (if-let [snapshot (:snapshot (last stream))]
      snapshot
      empty-stanpshot))


(defn append-step [stream step snapshot dt]
  (conj stream  (merge step {:snapshot snapshot, :dt dt})))



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
