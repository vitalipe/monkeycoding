(ns monkeycoding.editor.ui
    (:require
      [reagent.core :as r :refer [atom with-let cursor]]

      [monkeycoding.editor.codemirror.editor   :refer [codemirror-editor]]
      [monkeycoding.editor.player              :refer [player]]
      [monkeycoding.editor.timeline            :refer [timeline-widget]]

      [monkeycoding.editor.stream     :as stream :refer [stream->playback]]
      [monkeycoding.editor.state      :as store :refer [editor-state]]))


;; widgets
(defn icon [name]
  (->> (case name
          :export      "android-share-alt"
          :commit      "merge"
          :redo        "forward"
          :undo        "reply"
          :record      "record"
          :menu        "navicon"
          :play        "play"
          :goto-next   "ios-fastforward"
          :goto-prv    "ios-rewind"
          :goto-start  "ios-skipbackward"
          :goto-end    "ios-skipforward"
          :timeline    "ios-pulse-strong"
          :close       "close"
          :add-mark    "ios-location"
          :delta-time  "android-stopwatch"
          name)

    (str "i" ".icon" ".ion-")
    (keyword)
    (vector)))


(defn toolbar-button
  ([icon-or-props] (if (map? icon-or-props)
                    (toolbar-button icon-or-props (:icon icon-or-props))
                    (toolbar-button {} icon-or-props)))
  ([{:keys [
            disabled  ;; disabled buttons don't trigger clicks
            on-click  ;; the normal rect handler
            selected  ;; selected buttons are styled differently
            class]}   ;; custom class names
    icon-name]        ;; icon name or an icon element
   (let [custom-classes (list
                          (when disabled " disabled")
                          (when selected " selected"))]
     [:span.toolbar-button {
                            :class (apply conj class custom-classes)
                            :on-click (if disabled identity on-click)}
                      (cond
                        (keyword? icon-name) [icon icon-name]
                        (string? icon-name) [icon icon-name]
                        :otherwise icon-name)])))


(defn toolbar-spacer []
  [:span.toolbar-spacer])



(defn editor-screen []
  (let [{:keys [current-mode
                snapshot
                recording-highlight
                position recording]} @editor-state
        last-index (count (rest (:inputs recording)))]

    [:div.editor-screen-layout

        ;; editor header
        [:nav.editor-navbar.navbar.top
          [:div.form-inline
            [toolbar-button :menu]
            [:a.navbar-brand "Monkey Coding Editor (alpha)"]]


          [:div.mode-toobar.form-inline

            (when (= current-mode :recording-mode)
              [:div.recording-toobar.form-inline
                [toolbar-button {:on-click store/finish-recording} :close]
                [toolbar-spacer]
                [toolbar-button {:selected recording-highlight :on-click store/toggle-record-highlight} :add-mark]
                [toolbar-button {:on-click store/toggle-record-highlight} :delta-time]
                [icon "ios-arrow-down"]])

            (when-not (= current-mode :recording-mode)
              [:div.project-title-menu
                [:a.navbar-brand "New Project Name"]
                [icon "ios-arrow-down"]])]

          [:div.btn-group.project-toobar.form-inline
            [toolbar-button {:on-click store/start-recording} :record]


            [toolbar-spacer]

            [toolbar-button :undo]
            [toolbar-button :redo]

            [toolbar-spacer]
            [toolbar-button :commit]
            [toolbar-spacer]
            [toolbar-button :export]]]


        [:div.stage-container
          [:div.code-area
            (cond
              (= current-mode :playback-mode) [player {
                                                        :paused false
                                                        :playback (stream->playback recording)}]
              :otherwise [codemirror-editor {
                                              :text (:text snapshot)
                                              :selection (:selection snapshot)
                                              :marks  (:marks snapshot)
                                              :dt-cap 100
                                              :on-input store/record-input
                                              :on-highlight store/record-highlight
                                              :recording-highlight recording-highlight
                                              :read-only (= current-mode :default-mode)}])]]


        [:div.timeline-container
          [timeline-widget   {
                              :position position
                              :stream recording
                              :on-seek #(.log js/console "seek!!")}]

          ;; timeline controls
          [:nav.timeline-navbar.navbar.bottom
            [:div.timeline-toggle.form-inline
              [toolbar-button :timeline]]

            [:div.timeline-controls.form-inline
              [toolbar-button {:disabled (= 0 position) :on-click #(store/goto-postition 0)}   :goto-start]
              [toolbar-button {:disabled (= 0 position) :on-click #(store/previous-postition)} :goto-prv]

              (if (= current-mode :playback-mode)
                [toolbar-button {:on-click store/stop-playback} "stop"]
                [toolbar-button {:on-click store/start-playback} :play])

              [toolbar-button {:disabled (= last-index position) :on-click #(store/next-postition)}            :goto-next]
              [toolbar-button {:disabled (= last-index position) :on-click #(store/goto-postition last-index)} :goto-end]]]]]))
