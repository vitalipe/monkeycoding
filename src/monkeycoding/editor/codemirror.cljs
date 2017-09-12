(ns monkeycoding.editor.codemirror
    (:require
      [reagent.core :as r]
      [monkeycoding.player :refer [Player]]
      [cljsjs.codemirror]
      [cljsjs.codemirror.mode.javascript]))


(def default-config {
                      :mode "javascript"
                      :theme "twilight"
                      :lineNumbers true})


;; helper functions
(defn- noop [] nil)

(defn- create-codemirror! [dom-node config]
  (new  js/CodeMirror dom-node (clj->js (merge default-config config))))


(defn- init! [{:keys [
                      dom-node
                      initial-config
                      input-proxy]}]

  (doto (create-codemirror! dom-node initial-config)
    (.on "change" #(input-proxy (.getValue %1) %2))
    (.on "cursorActivity" #(input-proxy (.getValue %) (.getCursor %)))))



(defn- cm-input-data->event [text event dt]
  (let [
        input (or (js->clj event))
        pos (or (get input "from") event)
        origin (get input "origin")

        text-event? (partial contains? #{"+input" "+delete" "cut" "paste" "copy" "undo" "redo"})]

    (merge {
              :dt dt
              :snapshot text
              :position {:line (.-line pos) :ch (.-ch pos)}}

          (cond
            (nil? origin)           {:type :cursor}
            (text-event? origin)    {:type :input
                                     :insert (clojure.string/join "\n" (input "text"))
                                     :remove (count (clojure.string/join "\n" (input "removed")))}))))



(defn- component [spec]
  (r/create-class {
                    :component-did-mount (:on-mount spec)
                    :component-will-receive-props #((:on-props spec) (second %2))
                    :reagent-render (:render spec)}))


(defn- redundant-event? [current prv]
  (or
    (nil? (:type current))
    (= (dissoc current :dt) (dissoc prv :dt))))

(defn- process-input-event! [input-state text input]
  (let [
        now       (.now js/Date)
        last-time (:last-time @input-state)
        dt        (if last-time (- now last-time) 0)
        callback  (@input-state :cb)
        event     (cm-input-data->event text input dt)]

    (when-not (redundant-event? event (:last @input-state))
      (do
        (swap! input-state assoc :last event)
        (swap! input-state assoc :last-time now)
        (callback event)))))


;; Player
(defn codemirror-player [{:keys [paused playback]}]
  (let [
        pl (atom nil)
        config (merge default-config {:playback playback :paused paused})]

    (component {
                :on-mount (fn [this] (reset! pl (new Player (r/dom-node this) (clj->js config))))
                :on-props (fn [{paused :paused}] (.setPaused @pl paused))
                :render (fn [] [:div.player-content])})))



;; Editor
(defn codemirror-editor [{:keys [
                                  text
                                  read-only
                                  on-change]}]
  (let [
        cm (atom nil)
        input-state (atom {:cb noop :last nil})]


      (component {
                  :on-mount (fn [this]
                              (do
                                (swap! input-state assoc :cb (or on-change noop))
                                (reset! cm
                                  (init! {
                                          :dom-node (r/dom-node this)
                                          :initial-config  {:value (or text "") :readOnly read-only}
                                          :input-proxy (partial process-input-event! input-state)}))))

                  :on-props (fn [{:keys [read-only text on-change]}]
                                (do
                                  (swap! input-state assoc :cb (or on-change noop))
                                  (set! (.. @cm -options -readOnly) read-only)
                                  (when text (.setValue @cm text))))

                  :render (fn [] [:div])})))
