(ns monkeycoding.editor.ui
    (:require
      [reagent.core :as r :refer [atom with-let cursor]]

      [monkeycoding.editor.common              :refer [as-component]]
      [monkeycoding.editor.player              :refer [player preview-player]]
      [monkeycoding.editor.timeline            :refer [timeline-panel]]

      [monkeycoding.editor.codemirror.recorder     :as recorder]
      [monkeycoding.editor.codemirror.highlighter  :as highlighter]


      [monkeycoding.editor.stream     :as stream :refer [stream->playback-snapshot stream->snapshot stream->playback]]
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
   (let [custom-classes (remove nil? [
                                      (when disabled " disabled")
                                      (when selected " selected")])]
     [:span.toolbar-button {
                            :class (apply conj class custom-classes)
                            :on-click (if disabled identity on-click)}
                      (cond
                        (keyword? icon-name) [icon icon-name]
                        (string? icon-name) [icon icon-name]
                        :otherwise icon-name)])))


(defn toolbar-spacer []
  [:span.toolbar-spacer])



(defn editable-label [{:keys [value on-change class] :or {on-change identity}}]
  (with-let [
              state (r/atom {:text value :value value})
              commit #(do
                        (swap! state assoc :value (:text @state))
                        (on-change (:text @state)))]

    (when-not (= (:value @state) value)
      (reset! state {:value value :text value}))

    [:input.editable-label {
                             :type "text"
                             :value (:text @state)
                             :on-key-down #(when (= 13 (.-keyCode %)) (.blur (.-target %)))
                             :on-blur commit
                             :on-click #(.select (.-target %))
                             :on-change #(swap! state assoc :text (.. % -target -value))
                             :class class}]))


(defn keyboard-shortcuts [& key-list]
  (let [
        keys (reduce-kv #(assoc %1 (set %2) %3) {} (apply hash-map key-list))
        event->keys #(-> #{
                            (when (.-ctrlKey %) :ctrl)
                            (when (.-shiftKey %) :shift)
                            (keyword (.-key %))}
                        (disj nil))

        handler (fn [evt]
                  (when-let [callback (get keys (event->keys evt))] (callback)))]

      (as-component {
                      :on-mount (fn [_]  (.addEventListener js/window "keydown" handler))
                      :on-unmount (fn [_] (.removeEventListener js/window "keydown" handler))
                      :render (fn [] [:div.keyboard-shortcuts])})))


(defn editor-screen []
  (with-let [state (r/atom {
                            :dt-cap 500
                            :timeline-open true})]
    (let [{:keys [current-mode
                  recording-highlight
                  position recording]} @editor-state

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

            (when (= current-mode :recording-mode)
              [:div.recording-toobar.form-inline
                [toolbar-button {:on-click store/finish-recording} :close]
                [toolbar-spacer]
                [toolbar-button {:selected recording-highlight :on-click store/toggle-record-highlight} :add-mark]
                [toolbar-button {:on-click store/toggle-record-highlight} :delta-time]
                [icon "ios-arrow-down"]])

            (when-not (= current-mode :recording-mode)
              [:div.project-title-menu
                [icon "ios-arrow-down"]
                [editable-label {
                                  :value (get-in @editor-state [:meta :title])
                                  :on-change #(swap! editor-state assoc-in [:meta :title] %)}]])]

          [:div.btn-group.project-toobar.form-inline
            [toolbar-button {
                              :on-click store/start-recording
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
            (cond
              (= current-mode :default-mode) [preview-player
                                                            {:playback (stream->playback-snapshot recording position)}]

              recording-highlight [highlighter/component {
                                                          :text (:text snapshot)
                                                          :selection (:selection snapshot)
                                                          :marks  (:marks snapshot)

                                                          :on-highlight store/record-highlight}]

              (= current-mode :recording-mode) [recorder/component {
                                                                    :text (:text snapshot)
                                                                    :selection (:selection snapshot)
                                                                    :marks  (:marks snapshot)

                                                                    :dt-cap (:dt-cap @state)
                                                                    :on-input store/record-input}]

              (= current-mode :playback-mode) [player {
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
