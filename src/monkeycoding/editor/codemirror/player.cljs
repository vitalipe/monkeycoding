(ns monkeycoding.editor.codemirror.player
    (:require
      [reagent.core :as r :refer [atom]]
      [monkeycoding.editor.codemirror.common :refer [as-component default-config]]
      [monkeycoding.player :refer [Player]]))



;; Player is just a nice React wrapper of the JS player
(defn codemirror-player [{:keys [paused playback]}]
  (let [
        pl (atom nil)
        config (merge default-config {:playback playback :paused paused})]

    (as-component {
                    :on-mount (fn [this] (reset! pl (new Player (r/dom-node this) (clj->js config))))
                    :on-props (fn [{paused :paused}] (.setPaused @pl paused))
                    :render (fn [] [:div.player-content])})))
