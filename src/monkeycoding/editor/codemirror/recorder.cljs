(ns monkeycoding.editor.codemirror.recorder
    (:require
      [reagent.core :as r :refer [atom]]
      [cljsjs.codemirror]

      ;; todo move it to init or something
      [cljsjs.codemirror.addon.scroll.simplescrollbars]
      [cljsjs.codemirror.mode.javascript]

      [monkeycoding.editor.common             :refer [default-config as-component]]
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


(defn- redundant-event? [{:keys [last-input selecting]} current]
    (or
      (empty-selection? current)
      (cursor-event-during-selection? selecting current)
      (shadow-cursor-after-input?  current last-input)
      (= current last-input)))


(defn- calc-dt
        ([now last] (calc-dt now last (* 999  1000)))
        ([now last dt-cap] (min dt-cap (if last (- now last) 0))))


(defn take-adjusted-snapshot [cm marks {type :type :as event}]
  (let [snapshot (snapshot/take-snapshot cm marks)]
    (cond
      (= type :selection) (assoc snapshot :selection (select-keys event [:from :to]))
      :otherwise snapshot)))


(defn- merge-input-data [state input now]
  (-> state
      (assoc :selecting (and
                          (= :selection (:type input))
                          (not (empty-selection? input))))
      (merge (when-not (redundant-event? state input) {:last-time now :last-input input}))))


(defn process-input-event [{:keys [on-input marks dt-cap]} state cm event]
  (let [
        now  (.now js/Date)
        dt (calc-dt now (:last-time state) dt-cap)
        snapshot (take-adjusted-snapshot cm marks event)
        new-state (merge-input-data state event now)
        changed   (not= (:last-input state) (:last-input new-state))]

    (when changed
      (on-input (:last-input new-state) snapshot dt))

    new-state))


(defn init-input-events! [codemirror callback]
  (doto codemirror
    (.on "change"                #(callback %1 (parse/js->step %2)))
    (.on "cursorActivity"        #(callback %1 (parse/js->step (.getCursor %1))))
    (.on "beforeSelectionChange" #(callback %1 (parse/js->step %2)))))


(defn disable-undo-redo! [codemirror]
  (.on codemirror "beforeChange" #(when (contains? #{"undo" "redo"} (.-origin %2)) (.cancel %2))))


(defn- create-codemirror! [dom-node config]
  (let [config (merge default-config config)]
    (new  js/CodeMirror dom-node (js-obj
                                    "lineNumbers" (:show-line-numbers config)
                                    "theme" (:theme config)
                                    "language" (:language config)
                                    "scrollbarStyle" "overlay"
                                    "coverGutterNextToScrollbar" true))))


(defn editor [{:keys [
                      text
                      selection
                      marks
                      dt-cap
                      on-input] :as intitial-props}]

  (let [
        cm    (atom nil)
        props (atom intitial-props)
        state (atom {
                     :last-input nil
                     :last-time  (.now js/Date)})]

      (as-component {
                      :on-mount (fn [this]
                                  (let [
                                        codemirror (create-codemirror! (r/dom-node this) {})
                                        input-callback #(reset! state (process-input-event @props @state %1 %2))]

                                      (->> (doto codemirror
                                              (disable-undo-redo!)
                                              (snapshot/apply-snapshot! intitial-props)
                                              (init-input-events! input-callback))
                                          (reset! cm))))


                      :on-props (fn [new-props]
                                  (reset! props new-props))
                                  ;;(snapshot/apply-snapshot! @cm new-props))

                      :render (fn [] [:div {:style {:height "100%"}}])})));