(ns monkeycoding.editor.screen
    (:require
      [reagent.core :as r :refer [atom with-let cursor]]

      [monkeycoding.editor.player              :refer [player preview-player]]
      [monkeycoding.editor.timeline            :refer [timeline-panel]]

      [monkeycoding.editor.codemirror              :as codemirror]
      [monkeycoding.editor.codemirror.recorder     :as recorder]
      [monkeycoding.editor.codemirror.highlighter  :as highlighter]
      [monkeycoding.editor.codemirror.textarea     :refer [json-text-area markdown-text-area]]


      [monkeycoding.editor.stream     :as stream :refer [stream->snapshot]]
      [monkeycoding.editor.state      :as store  :refer [editor-state]]
      [monkeycoding.editor.undo                  :refer [undo! redo! can-undo? can-redo?]]

      [monkeycoding.editor.exporting  :refer [export-modal]]


      [monkeycoding.widgets.keyboard :refer [keyboard-shortcuts]]
      [monkeycoding.widgets.icon     :refer [icon]]
      [monkeycoding.widgets.dropdown :refer [dropdown-text-item dropdown-submenu]]
      [monkeycoding.widgets.label    :refer [multi-select-label combo-label editable-label select-label]]
      [monkeycoding.widgets.modal    :refer [modal modal-header modal-content modal-footer]]
      [monkeycoding.widgets.scroll   :refer [scroll-panel]]
      [monkeycoding.widgets.option   :refer [dropdown-option boolean-option label-option]]
      [monkeycoding.widgets.toolbar  :refer [toolbar-spacer toolbar-button]]))



(def language-options-list (->> (vals codemirror/languages)
                             (map #(clojure.set/rename-keys % {:display-name :title}))
                             (sort-by :title)))


(defn- mark-data->json-text [mark]
  (.stringify js/JSON (clj->js (dissoc mark :id :info :inserted-at)) nil 2))


(defn- json-text->mark-data [text]
  (try
    (-> (.parse js/JSON text)
      (js->clj :keywordize-keys true)
      (dissoc :id :info :inserted-at))
    (catch :default e nil)))


(defn highlight-edit-modal[{:keys [on-close on-done mark]}]
  (with-let [
             state (r/atom {
                            :parse-error? false
                            :json-text (mark-data->json-text mark)
                            :info-text (:info mark)})
             on-done #(when-not (:parse-error?  @state)
                        (-> mark
                          (select-keys [:id :inserted-at])
                          (assoc :info (:info-text  @state))
                          (merge (json-text->mark-data (:json-text  @state)))
                          (on-done)))]

    [modal {:class "highlight-edit-modal"}
        [modal-header
         [:h4 "Edit Highlight"]]

        [modal-content
         [:h5 "Text:"]
         [markdown-text-area {
                              :class "mark-description"
                              :on-change #(swap! state assoc :info-text %)
                              :text (:info-text @state)}]

         [:div.code-export.playback-code
           [:div.meta-header
             [:h5 "JSON Metadata:"]
             [:label.parse-error {:class (when-not (:parse-error? @state) "hidden")}
               "JSON parse error!"]]
           [json-text-area
                      {
                        :class "json-edit"
                        :on-change #(swap! state assoc
                                            :json-text %
                                            :parse-error? (nil? (json-text->mark-data %)))
                        :text (:json-text @state)}]]]
        [modal-footer
          [:button.btn.btn-danger   {:on-click on-close}
           [icon :undo]
           " "
           (if (:parse-error? @state) "I've made a huge mistake!" "cancel")]
          [:button.btn.btn-success  {
                                     :on-click #(on-done mark)
                                     :disabled (:parse-error? @state)}
                [icon :ok] " " "save"]]]))


(defn add-highlight-modal[{:keys [on-close on-add mark]}]
  (with-let [mark-ref (r/atom mark)]
    [modal {:class "add-highlight-modal"}
        [modal-header
         [:h4 "Add Highlight"]]
        [modal-content
         [markdown-text-area {
                              :class "mark-description"
                              :on-change #(swap! mark-ref assoc :info %)
                              :text (:info @mark-ref)}]]
        [modal-footer
          [:button.btn.btn-danger  {:on-click on-close}            [icon :delete] " " "discard"]
          [:button.btn.btn-success {:on-click #(on-add @mark-ref)} [icon "plus"]     " " "create"]]]))


(defn settings-modal[{:keys [config on-change on-close]}]
    [modal {
            :class "config-modal"
            :on-close on-close}

        [modal-header
          [:label.h4 "Settings"]]

        [modal-content
         [:div.option-items
            [dropdown-option {
                               :title "editor theme:"
                               :on-select #()
                               :selected "seti"
                               :options [{:value "seti" :title "seti" :selected true}]}]

            [dropdown-option {
                              :title "language:"
                              :on-select #(on-change (assoc config :language %))
                              :selected (:language config)
                              :options language-options-list}]

            [boolean-option {
                             :title "show line numers:"
                             :value (:show-line-numbers config)
                             :on-change #(on-change (assoc config :show-line-numbers %))}]

            [boolean-option {
                             :title "show border hints:"
                             :value false
                             :on-change #()}]]]])


(defn about-modal[{:keys [on-close]}]
    [modal {
            :class "about-modal"
            :on-close on-close}

        [modal-header
          [:label.h4 "About"]]

        [modal-content
         [:div "todo!"]]])


(defn- marks-panel [{:keys [open marks position on-marks-change on-mark-delete]}]
  (with-let [
              editing-mark (r/atom nil)
              tab-state (r/atom :all)
              active? #(= %1 (:inserted-at %2))]

    [:div.marks-panel.side-panel {:class (when open "open")}
      (when @editing-mark
        [highlight-edit-modal {
                               :on-close #(reset! editing-mark nil)
                               :on-done  #(do
                                            (on-marks-change (assoc marks (:id %) %))
                                            (reset! editing-mark nil))
                               :mark @editing-mark}])
      [:div.tabs
        [:div.tab.left  {
                          :on-click #(reset! tab-state :all)
                          :class (when (= :all @tab-state) "selected")}  "all"]
        [:div.tab.right {
                          :on-click #(reset! tab-state :active)
                          :class (when (= :active @tab-state) "selected")} "frame"]]

      [scroll-panel
        [:div.mark-list
         (let [active-id (:id @editing-mark)]
           (->> (vals marks)
             (filter (if (= :active @tab-state) (partial active? position) identity))
             (map (fn [{:keys [id inserted-at remove info] :as mark}]
                     [:div.mark-list-item {
                                           :on-click #(reset! editing-mark mark)
                                           :key id
                                           :class [
                                                   (when (= id active-id) "being-edited")
                                                   (when (active? position mark) "active")]}
                       [:div.header
                         [:div.header-icons
                           [:label.preview  (str " " id) " " [icon :add-mark]]
                           [:label.insert   (str (inc inserted-at) " ") " " [icon :record]]]
                         [:div.header-buttons
                           [:span {:on-click #(do
                                                (.stopPropagation %)
                                                (on-mark-delete id))}
                            [icon :delete]]]]



                       [:div.info-preview info]]))))]]]))


(defn editor-screen []
  (with-let [state (r/atom {
                            :next-highlight nil
                            :dt-cap 500

                            :export-open false
                            :settings-open false
                            :about-open false

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

        (cond
          (:next-highlight @state) [add-highlight-modal {
                                                         :mark stream/empty-mark-data
                                                         :on-close #(swap! state assoc :next-highlight nil)
                                                         :on-add  #(let [{:keys [from to]} (:next-highlight @state)]
                                                                     (store/record-highlight from to %)
                                                                     (swap! state assoc :next-highlight nil))}]
          (:export-open @state) [export-modal {
                                                :config config
                                                :recording recording
                                                :on-close #(swap! state assoc :export-open false)}]

          (:settings-open @state) [settings-modal {
                                                    :on-change #(swap! editor-state assoc :config %)
                                                    :config config
                                                    :on-close #(swap! state assoc :settings-open false)}]

          (:about-open @state) [about-modal {
                                              :on-close #(swap! state assoc :about-open false)}]

          (not= current-mode :playback-mode) [keyboard-shortcuts
                                              [:ctrl :z] undo!
                                              [:ctrl :y] redo!])

        ;; editor header
        [:div.editor-navbar.navbar.top
          [:div.form-inline
            [toolbar-button :menu]
            [:a.navbar-brand "Monkey Coding Editor (alpha)"]]

          [:div.mode-toobar.form-inline
            (case current-mode
              :recording-mode
                  [:div.recording-toobar.form-inline
                    [multi-select-label {:on-select #(swap! state assoc :dt-cap %)}
                     [select-label {:value Infinity :text "no cap"  :selected (= Infinity (:dt-cap @state))}]
                     [select-label {:value 100      :text ".1s"     :selected (= 100      (:dt-cap @state))}]
                     [select-label {:value 500      :text ".5s"     :selected (= 500      (:dt-cap @state))}]
                     [select-label {:value 1000     :text "1s"      :selected (= 1000     (:dt-cap @state))}]]]

              :highlighting-mode
                  [:label "select any text region..."]

              :playback-mode
                  [multi-select-label {:on-select #(swap! editor-state assoc-in [:config :playback-speed] %)}
                     [select-label {:value 0.5 :text "0.5x"  :selected (= 0.5  (:playback-speed config))}]
                     [select-label {:value 1   :text "1x"    :selected (= 1    (:playback-speed config))}]
                     [select-label {:value 1.5 :text "1.5x"  :selected (= 1.5  (:playback-speed config))}]
                     [select-label {:value 2   :text "2x"    :selected (= 2    (:playback-speed config))}]]


              ;; default
              [combo-label {
                            :text title
                            :on-text-change store/rename}
                [dropdown-text-item {
                                     :key "settings"
                                     :text "Settings"
                                     :icon :settings
                                     :on-click #(swap! state assoc :settings-open true)}]
                [dropdown-submenu {:key "actions" :text "Recording" :icon :recording}
                 [dropdown-text-item {
                                      :key "Squash"
                                      :text "Squash"
                                      :disabled (> 0 position)
                                      :on-click store/squash
                                      :icon :squash}]
                 [dropdown-text-item {
                                      :key "Discard"
                                      :text "Discard"
                                      :icon :delete
                                      :on-click store/reset
                                      :disabled (empty? (:inputs recording))}]
                 [dropdown-text-item {
                                      :key "Strip Marks"
                                      :text "Strip Marks"
                                      :disabled true}]]
                [dropdown-text-item {
                                     :key "About"
                                     :text "About"
                                     :icon :about
                                     :on-click #(swap! state assoc :about-open  true)}]])]

          [:div.btn-group.project-toobar.form-inline
            [toolbar-button {
                              :on-click store/toggle-recording
                              :selected (= :recording-mode current-mode)
                              :icon :record}]
            [toolbar-button {
                             :selected (= current-mode :highlighting-mode)
                             :on-click store/toggle-record-highlight
                             :disabled (empty? (:inputs recording))
                             :icon :add-mark}]
            [toolbar-spacer]

            (if (= current-mode :playback-mode)
              [toolbar-button {:on-click store/stop-playback} :pause]
              [toolbar-button {
                                :icon :play
                                :disabled (empty? (:inputs recording))
                                :on-click store/start-playback}])
            [toolbar-spacer]
            [toolbar-button {
                              :icon :undo
                              :disabled (not (can-undo?))
                              :on-click undo!}]
            [toolbar-button {
                              :icon :redo
                              :disabled (not (can-redo?))
                              :on-click redo!}]
            [toolbar-spacer]
            [toolbar-button {
                              :icon :export
                              :on-click #(swap! state assoc :export-open true)}]
            [toolbar-spacer]
            [toolbar-button {
                              :selected (:side-panel-open @state)
                              :on-click #(swap! state update :side-panel-open not)
                              :icon :marks}]]]

        [:div.stage-container
          [:div.code-area
            (case current-mode
              :preview-mode [preview-player {
                                             :config config
                                             :position position
                                             :recording recording}]
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
                                      :on-done store/stop-playback
                                      :on-progress store/update-player-progress
                                      :position position
                                      :recording recording}])]
          [marks-panel
            {
              :marks (:marks-data recording)
              :on-marks-change store/update-marks
              :on-mark-delete store/delete-mark
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
            [toolbar-button {:disabled (> 0 position) :on-click #(store/goto-postition -1)}   :goto-start]
            [toolbar-button {:disabled (> 0 position) :on-click #(store/previous-postition)} :goto-prv]

            (if (empty? (:inputs recording))
              [:label "0/0"]
              [:label (str (inc position) "/" (count (:inputs recording)))])

            [toolbar-button {:disabled (= last-index position) :on-click #(store/next-postition)}            :goto-next]
            [toolbar-button {:disabled (= last-index position) :on-click #(store/goto-postition last-index)} :goto-end]]]])))
