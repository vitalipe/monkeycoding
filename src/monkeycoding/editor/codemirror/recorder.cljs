(ns monkeycoding.editor.codemirror.recorder
    (:require
      [reagent.core :as r :refer [atom]]

      [monkeycoding.editor.codemirror         :refer [create-codemirror!]]
      [monkeycoding.util                      :refer [as-component]]
      [monkeycoding.editor.codemirror.parse   :as parse]
      [monkeycoding.editor.codemirror.snapshot   :as snapshot]))


(defn- find-position-after-input [{ {:keys [ch line]} :position  text :insert}]
  (let [
        additional-rows  (count (re-seq #"\n" text))
        additional-chars (count (last (clojure.string/split  text #"\n")))]
    {
      :line (+ additional-rows line)
      :ch   (+ additional-chars (if (= additional-rows 0) ch 0))}))


(defn- empty-selection? [{:keys [type from to]}]
  (and
      (= type type :selection)
      (= from to)))


(defn- cursor-event-during-selection? [selecting {type :type}]
  (and selecting (= type :cursor)))


(defn- shadow-cursor-after-input? [cur prv]
  (and
    (= (:type cur) :cursor)
    (= (:type prv) :input)
    (= (cur :position) (find-position-after-input prv))))


(defn- redundant-event? [next last selecting]
    (or
      (empty-selection? next)
      (cursor-event-during-selection? selecting next)
      (shadow-cursor-after-input?  next last)
      (= next last)))


(defn- calc-dt
        ([now last] (calc-dt now last (* 999  1000)))
        ([now last dt-cap] (min dt-cap (if last (- now last) 0))))


(defn take-adjusted-snapshot [cm marks {type :type :as event}]
  (let [
        snapshot (snapshot/take-snapshot cm marks)
        selection-from-event (select-keys event [:from :to])]

    (assoc snapshot :selection
      (if (= type :selection)  selection-from-event
                               (:selection snapshot)))))


(defn process-input-event [
                            {:keys [on-input marks dt-cap]}
                            {:keys [selecting last-input last-time] :as state}
                            codemirror
                            next-input]

  (let [now  (.now js/Date)]
    (merge state {:last-input next-input}
      (when-not (redundant-event? next-input last-input selecting)
        (on-input next-input
                             (take-adjusted-snapshot codemirror marks next-input)
                             (calc-dt now (:last-time state) dt-cap))
        {
          :last-time now
          :selecting  (= :selection (:type next-input))}))))


(defn init-input-events! [codemirror callback]
  (doto codemirror
    (.on "change"                #(callback %1 (parse/js->input %2)))
    (.on "cursorActivity"        #(callback %1 (parse/js->input (.getCursor %1))))
    (.on "beforeSelectionChange" #(callback %1 (parse/js->input %2)))))


(defn disable-undo-redo-events! [codemirror]
  (.on codemirror "beforeChange" #(when (contains? #{"undo" "redo"} (.-origin %2)) (.cancel %2))))


(defn component [{:keys [
                          text
                          selection
                          marks
                          dt-cap
                          config
                          on-input] :as intitial-props}]

  (let [
        cm    (atom nil)
        props (atom intitial-props)
        state (atom {
                     :preforming-snapshot false
                     :last-input nil
                     :last-time  (.now js/Date)})]

      (as-component {
                      :on-mount (fn [this]
                                  (let [
                                        codemirror (create-codemirror! (r/dom-node this) config)
                                        input-callback #(when-not (:preforming-snapshot @state)
                                                          (reset! state (process-input-event @props @state %1 %2)))]

                                      (->> (doto codemirror
                                              (disable-undo-redo-events!)
                                              (snapshot/apply-snapshot! intitial-props)
                                              (init-input-events! input-callback))
                                          (reset! cm))))


                      :on-props (fn [new-props]
                                  (reset! props new-props)
                                  (swap! state assoc :preforming-snapshot true)
                                  (snapshot/apply-snapshot! @cm new-props)
                                  (swap! state assoc :preforming-snapshot false))


                      :render (fn [] [:div {:style {:height "100%"}}])})));
