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
    (.on "cursorActivity" #(input-proxy (.getValue %) (.getCursor %)))
    (.on "beforeSelectionChange" #(input-proxy (.getValue %1) %2))))


(defn- extract-position [js-pos]
  {:line (.-line js-pos) :ch (.-ch js-pos)})

(defn- cm-input-data->event [text event dt]
  (let [
        input  (js->clj event)
        origin (get input "origin")

        cursor-event? (nil? origin)
        selection-event? (contains? input "ranges")
        text-event? (contains? #{"+input" "+delete" "cut" "paste" "copy" "undo" "redo"} origin)]

    (merge {
              :dt dt
              :snapshot text}

          (cond
            cursor-event?  {
                            :type :cursor
                            :position (extract-position event)}


            selection-event? {
                              :type :selection
                              :head   (extract-position (.-head (first (input "ranges"))))
                              :anchor (extract-position (.-anchor (first (input "ranges"))))}


            text-event?    {
                             :type :input
                             :insert (clojure.string/join "\n" (input "text"))
                             :remove (count (clojure.string/join "\n" (input "removed")))
                             :position (extract-position (get input "from"))}))))



(defn- component [spec]
  (r/create-class {
                    :component-did-mount (:on-mount spec)
                    :component-will-receive-props #((:on-props spec) (second %2))
                    :reagent-render (:render spec)}))



(defn in-selecting-state? [{:keys [last last-selection]}]
  (and
    (not= (:head last-selection) (:anchor last-selection))
    (= last last-selection)))

(defn- redundant-event? [state current prv]
  (or
    (and (= (current :type) :cursor) (in-selecting-state? state))
    (nil? (:type current))
    (= (dissoc current :dt) (dissoc prv :dt))))


(defn- process-input-event! [input-state text input]
  (let [
        now       (.now js/Date)
        last-time (:last-time @input-state)
        dt        (if last-time (- now last-time) 0)
        callback  (@input-state :cb)
        event     (cm-input-data->event text input dt)
        target    (if (= (event :type) :selection) :last-selection :last-input)]

    (when-not (redundant-event? @input-state event (@input-state target))
      (do
        (swap! input-state assoc target event)
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
        input-state (atom {
                            :cb noop
                            :last nil
                            :last-input nil
                            :last-selection nil})]


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
