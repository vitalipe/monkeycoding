(ns monkeycoding.editor.screen
    (:require
      [reagent.core :as r :refer [atom with-let cursor]]

      [monkeycoding.util                       :refer [as-component]]
      [monkeycoding.editor.player              :refer [player preview-player]]
      [monkeycoding.editor.timeline            :refer [timeline-panel]]

      [monkeycoding.editor.codemirror.recorder     :as recorder]
      [monkeycoding.editor.codemirror.highlighter  :as highlighter]


      [monkeycoding.editor.stream     :as stream :refer [stream->playback-snapshot stream->snapshot stream->playback]]
      [monkeycoding.editor.state      :as store :refer [editor-state]]

      [monkeycoding.widgets :refer [
                                    keyboard-shortcuts
                                    icon
                                    editable-label
                                    toolbar-button
                                    toolbar-spacer]]))


(defn editor-screen []
  (with-let [state (r/atom {
                            :dt-cap 500
                            :timeline-open true})]
    (let [{:keys [current-mode position recording]} @editor-state
          last-index (dec (count (:inputs recording)))
          snapshot (stream->snapshot recording position)]

      [:div.editor-screen-layout
        (when-not (= current-mode :playback-mode)
          [keyboard-shortcuts
              [:ctrl :z] store/undo!
              [:ctrl :y] store/redo!])


        ;; editor header
        [:nav.editor-navbar.navbar.top
          [:div.form-inline
            [toolbar-button :menu]
            [:a.navbar-brand "Monkey Coding Editor (alpha)"]]


          [:div.mode-toobar.form-inline

            (if (contains? #{:recording-mode :highlighting-mode} current-mode)
              [:div.recording-toobar.form-inline
                [toolbar-button {:on-click store/finish-recording} :close]
                [toolbar-spacer]
                [toolbar-button {:selected (= current-mode :highlighting-mode) :on-click store/toggle-record-highlight} :add-mark]
                [toolbar-button {:on-click store/toggle-record-highlight} :delta-time]
                [icon "ios-arrow-down"]]
              ;; else
              [:div.project-title-menu
                [icon "ios-arrow-down"]
                [editable-label {
                                  :value (get-in @editor-state [:meta :title])
                                  :on-change #(swap! editor-state assoc-in [:meta :title] %)}]])]

          [:div.btn-group.project-toobar.form-inline
            [toolbar-button {
                              :on-click store/toggle-recording
                              :selected (= current-mode :recording-mode)
                              :icon :record}]

            (if (= current-mode :playback-mode)
              [toolbar-button {:on-click store/stop-playback} "stop"]
              [toolbar-button {
                                :icon :play
                                :disabled (empty? (:inputs recording))
                                :on-click store/start-playback}])

            [toolbar-spacer]

            [toolbar-button {
                              :icon :undo
                              :disabled (not (store/can-undo?))
                              :on-click store/undo!}]

            [toolbar-button {
                              :icon :redo
                              :disabled (not (store/can-redo?))
                              :on-click store/redo!}]

            [toolbar-spacer]
            [toolbar-button :export]
            [toolbar-spacer]
            [toolbar-button :add-mark]]]


        [:div.stage-container
          [:div.code-area

            (case current-mode
              :preview-mode [preview-player {:playback (stream->playback-snapshot recording position)}]
              :highlighting-mode [highlighter/component {
                                                          :text (:text snapshot)
                                                          :selection (:selection snapshot)
                                                          :marks  (:marks snapshot)

                                                          :on-highlight store/record-highlight}]
              :recording-mode [recorder/component {
                                                    :text (:text snapshot)
                                                    :selection (:selection snapshot)
                                                    :marks  (:marks snapshot)

                                                    :dt-cap (:dt-cap @state)
                                                    :on-input store/record-input}]
              :playback-mode [player {
                                      :paused false
                                      :on-progress #(store/update-player-progress %)
                                      :playback (stream->playback recording)}])]]


        [timeline-panel   {
                            :open (:timeline-open @state)
                            :position position
                            :stream recording
                            :on-seek store/goto-postition}]


        ;; timeline controls
        [:nav.timeline-navbar.navbar.bottom {:draggable false}
          [:div.timeline-toggle.form-inline
            [toolbar-button {:selected (:timeline-open @state)
                             :on-click #(swap! state update :timeline-open not)
                             :icon :timeline}]]

          [:div.timeline-controls.form-inline
            [toolbar-button {:disabled (> 1 position) :on-click #(store/goto-postition 0)}   :goto-start]
            [toolbar-button {:disabled (> 1 position) :on-click #(store/previous-postition)} :goto-prv]

            (if (empty? (:inputs recording))
              [:label "0/0"]
              [:label (str (inc position) "/" (count (:inputs recording)))])

            [toolbar-button {:disabled (= last-index position) :on-click #(store/next-postition)}            :goto-next]
            [toolbar-button {:disabled (= last-index position) :on-click #(store/goto-postition last-index)} :goto-end]]]])))
