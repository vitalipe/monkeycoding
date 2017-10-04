(ns monkeycoding.editor.ui
    (:require
      [reagent.core :as r :refer [atom with-let cursor]]

      [monkeycoding.editor.codemirror.editor   :refer [codemirror-editor]]
      [monkeycoding.editor.player              :refer [player]]
      [monkeycoding.editor.timeline            :refer [timeline-widget]]

      [monkeycoding.editor.stream     :as stream :refer [stream->playback]]
      [monkeycoding.editor.state      :as store :refer [editor-state]]))


(defn- event->int [evt]
  (.parseInt js/window (.. evt -target -value)))


(defn marks-panel [marks-cursor]
  [:ul
      (map #(vector :li {:key (:id %)} (str %)) (vals @marks-cursor))])



(defn recording-mode []
  (with-let [dt-cap (atom 1000)]
    (let [
          recording-highlight (:recording-highlight @editor-state)
          snapshot (:snapshot @editor-state)]
      [:div
          [:div.toolbar
            [:button {:on-click store/finish-recording} "finish"]
            [:button {:on-click store/toggle-record-highlight} (if recording-highlight "cancel" "highlight")]
            [:div
              [:label "delay cap: "]
              [:select {:value @dt-cap :on-change #(reset! dt-cap (event->int %))}
                [:option {:value 100} "100ms"]
                [:option {:value 500} "500ms"]
                [:option {:value 1000} "1000ms"]]]]
          [:div.code-area
            [codemirror-editor {
                                :text (:text snapshot)
                                :selection (:selection snapshot)
                                :marks (:marks snapshot)

                                :dt-cap @dt-cap

                                :on-input store/record-input
                                :on-highlight store/record-highlight
                                :recording-highlight recording-highlight}]]])))


(defn default-mode []
    [:div
      [:div.toolbar
        [:button {:on-click store/start-recording} "record"]
        [:button {:on-click store/start-playback} "play"]
        [:button {:on-click store/discard-recording} "reset"]
        [:button {:disabled true } "export"]
        [:button {:on-click store/previous-postition} "<|"]
        [:button {:on-click store/next-postition} "|>"]]

      [:div.code-area
        [codemirror-editor {
                            :read-only true
                            :text (get-in  @editor-state [:snapshot :text])
                            :selection (get-in  @editor-state [:snapshot :selection])
                            :marks (get-in  @editor-state [:snapshot :marks])}]]])


(defn playback-mode []
  (r/with-let [paused (r/atom false)]
    [:div
      [:div.toolbar
        [:button {:on-click store/stop-playback} "stop"]
        [:button {:on-click #(swap! paused not)} (if @paused "resume" "pause")]]
      [:div.code-area
        [player {
                  :paused @paused
                  :playback (stream->playback (:recording @editor-state))}]]]))


(defn editor-screen []
  (let [mode (:current-mode @editor-state)]
    [:nav
      [:h3 "Monkey Coding Editor"]


      [:section
        (case mode
            :default-mode   [default-mode]
            :recording-mode [recording-mode]
            :playback-mode  [playback-mode])


        [timeline-widget
          {
            :position (:position @editor-state)
            :stream (:recording @editor-state)
            :on-seek #(.log js/console "seek")}]

        [marks-panel (cursor editor-state [:recording :marks])]]]))
