(ns monkeycoding.editor.ui
    (:require
      [reagent.core :as r]

      [monkeycoding.editor.codemirror.editor :refer [codemirror-editor]]
      [monkeycoding.editor.codemirror.player :refer [codemirror-player]]

      [monkeycoding.editor.stream     :as stream :refer [stream->playback]]
      [monkeycoding.editor.state      :as store :refer [editor-state]]))


(defn recording-mode []
  [:div
      [:div.toolbar
        [:button {:on-click store/finish-recording}       "finish"]
        [:button {:on-click store/start-record-highlight} "highlight"]]
      [:div.code-area
        [codemirror-editor {
                            :text (:text @editor-state)
                            :on-input store/record-input

                            :on-highlight store/record-highlight
                            :recording-highlight (:recording-highlight  @editor-state)}]]])


(defn default-mode []
    [:div
      [:div.toolbar
        [:button {:on-click store/start-recording} "record"]
        [:button {:on-click store/start-playback} "play"]
        [:button {:on-click store/discard-recording} "reset"]
        [:button {:disabled true } "export"]]
      [:div.code-area
        [codemirror-editor {
                            :read-only true
                            :text (:text @editor-state)}]]])

(defn playback-mode []
  (r/with-let [paused (r/atom false)]
    [:div
      [:div.toolbar
        [:button {:on-click store/stop-playback} "stop"]
        [:button {:on-click #(swap! paused not)} (if @paused "resume" "pause")]]
      [:div.code-area
        [codemirror-player {
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
            :playback-mode  [playback-mode])]]))
