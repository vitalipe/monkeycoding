(ns monkeycoding.editor.stream)


(def empty-stream
          {
            :inputs []
            :selection []})


(defn merge-streams [stream-one stream-two]
  (merge-with #(into [] (concat %1 %2)) stream-one stream-two))


(defn stream->playback [stream]
  {
   :initial (select-keys (first (:inputs stream)) [:snapshot :at])
   :inputs (into [] (map #(dissoc % :snapshot) (rest (:inputs stream))))
   :selection (rest (:selection stream))})
