(ns monkeycoding.editor.exporting
    (:require
      [reagent.core :as r :refer [atom with-let cursor]]

      [monkeycoding.editor.codemirror.exporter     :as exporter]
      [monkeycoding.editor.codemirror.preview      :refer [js-preview html-preview]]

      [monkeycoding.widgets.dropdown :refer [dropdown-text-item]]
      [monkeycoding.widgets.label    :refer [combo-label]]
      [monkeycoding.widgets.modal    :refer [modal modal-header modal-content]]
      [monkeycoding.widgets.option   :refer [dropdown-option boolean-option label-option]]))


(def theme-options-list (->> (vals exporter/themes)
                          (map #(clojure.set/rename-keys % {:display-name :title}))
                          (sort-by :title)))


(defn export-modal[{:keys [recording config on-close]}]
  (with-let [options (r/atom (merge exporter/default-options config))]
    [modal {
            :class "export-modal"
            :on-close on-close}

        [modal-header
          [combo-label {:class "h4" :text "CodeMirror Exporter (default)"}
            [dropdown-text-item {:key 0 :checked true :text "CodeMirror Exporter"}]
            [dropdown-text-item {:key 1 :disabled true :text "Add Exporter.."}]]]

        [modal-content
          [:div.option-items
            [dropdown-option {
                              :title "theme:"
                              :on-select #(swap! options assoc :theme %)
                              :selected (:theme @options)
                              :options theme-options-list}]
            [boolean-option {
                              :title "show highlights:"
                              :value (:show-hightlights @options)
                              :on-change #(swap! options assoc :show-hightlights %)}]
            [boolean-option {
                              :title "show line numbers:"
                              :value (:show-line-numbers @options)
                              :on-change #(swap! options assoc :show-line-numbers %)}]

            [dropdown-option {
                              :title "playback speed:"
                              :on-select #(swap! options assoc :playback-speed %)
                              :selected (:playback-speed @options)
                              :options [
                                        {:title ".5x"  :value 0.5}
                                        {:title "1x"   :value 1}
                                        {:title "1.5x" :value 1.5}
                                        {:title "2x"   :value 2}]}]

            [label-option {
                            :title "parent selector:"
                            :value (:parent-selector @options)
                            :on-edit #(swap! options assoc :parent-selector %)}]]

          [:div.code-export.playback-code
            [:h5 "Playback:"]
            [js-preview (exporter/compile-playback @options recording)]]
          [:div.code-export.dependecies-code
            [:h5 "Dependecies:"]
            [html-preview (exporter/compile-dependecies @options)]]]]))
