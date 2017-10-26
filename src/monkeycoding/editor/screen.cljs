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
                                    modal
                                    modal-content
                                    modal-footer
                                    toolbar-spacer]]))


(defn- marks-panel [{:keys [open marks position]}]
  (with-let [
              tab-state (r/atom :all)
              active? (fn [pos {:keys [remove insert]}] (<= insert pos (dec (or remove Infinity))))]


    [:div.marks-panel.side-panel {:class (when open "open")}
      [:div.tabs
        [:div.tab.left  {
                          :on-click #(reset! tab-state :all)
                          :class (when (= :all @tab-state) "selected")}  "all"]
        [:div.tab.right {
                          :on-click #(reset! tab-state :active)
                          :class (when (= :active @tab-state) "selected")} "active"]]

      [:ul.mark-list
        (->> (vals marks)
          (filter (if (= :active @tab-state) (partial active? position) identity))
          (map (fn [{:keys [id insert remove info] :as mark}]
                  [:li.mark-list-item {:key id :class (when (active? position mark) "active")}
                    [:div.header
                      [:label.preview [icon :add-mark] (str " " id)]
                      [:label.insert (str (inc insert) " ") [icon :record]]]
                    [:div.info-preview info]])))]]))


(defn add-highlight-modal[{:keys [on-close on-add]}]
  (with-let [info (r/atom "add description...")]
    [modal {
            :class "add-highlight-modal"
            :on-close on-close}

        [modal-content
          [editable-label {
                            :on-change #(reset! info %)
                            :value @info}]]
        [modal-footer
          [:button.btn.btn-primary {:on-click #(on-add @info)} "add"]]]))




(defn editor-screen []
  (with-let [state (r/atom {
                            :next-highlight nil
                            :dt-cap 500
                            :timeline-open false
                            :side-panel-open false})]
    (let [{:keys [
                  current-mode
                  position
                  title
                  config
                  recording]} @editor-state

          last-index (dec (count (:inputs recording)))
          snapshot (stream->snapshot recording position)]

      [:div.editor-screen-layout

        (when-not (= current-mode :playback-mode)
          [keyboard-shortcuts
              [:ctrl :z] store/undo!
              [:ctrl :y] store/redo!])

        (when (:next-highlight @state)
          [add-highlight-modal {
                                :on-close #(swap! state assoc :next-highlight nil)
                                :on-add  #(do
                                              (store/record-highlight (:next-highlight @state) %)
                                              (swap! state assoc :next-highlight nil))}])
        ;; editor header
        [:div.editor-navbar.navbar.top
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
                                  :value title
                                  :on-change store/rename}]])]

          [:div.btn-group.project-toobar.form-inline
            [toolbar-button {
                              :on-click store/toggle-recording
                              :selected (contains? #{:recording-mode :highlighting-mode} current-mode)
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
            [toolbar-button {
                              :selected (:side-panel-open @state)
                              :on-click #(swap! state update :side-panel-open not)
                              :icon :add-mark}]]]



        [:div.stage-container
          [:div.code-area

            (case current-mode
              :preview-mode [preview-player {:config config :playback (stream->playback-snapshot recording position)}]
              :highlighting-mode [highlighter/component {
                                                          :text (:text snapshot)
                                                          :selection (:selection snapshot)
                                                          :marks  (:marks snapshot)
                                                          :config config

                                                          :on-highlight #(swap! state assoc :next-highlight {:from %1 :to %2})}]
              :recording-mode [recorder/component {
                                                    :text (:text snapshot)
                                                    :selection (:selection snapshot)
                                                    :marks  (:marks snapshot)
                                                    :config config

                                                    :dt-cap (:dt-cap @state)
                                                    :on-input store/record-input}]
              :playback-mode [player {
                                      :paused false
                                      :config config
                                      :on-progress #(store/update-player-progress %)
                                      :playback (stream->playback recording)}])]

          [marks-panel
            {
              :marks (:marks recording)
              :position position
              :open (:side-panel-open @state)}]]


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
