(ns monkeycoding.editor.ui
    (:require
      [reagent.core :as r]

      [monkeycoding.editor.codemirror :refer [codemirror-editor codemirror-player]]
      [monkeycoding.editor.stream     :as stream :refer [stream->playback]]
      [monkeycoding.editor.state      :as store :refer [editor-state]]))


(defn recording-mode []
  [:div
      [:div.toolbar
        [:button {:on-click store/finish-recording} "finish"]]
      [:div.code-area
        [codemirror-editor {
                            :text (:text @editor-state)
                            :on-change store/record-input}]]])


(defn default-mode []
    [:div
      [:div.toolbar
        [:button {:on-click store/start-recording} "record"]
        [:button {:on-click store/start-playback} "play"]
        [:button "export"]]
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
