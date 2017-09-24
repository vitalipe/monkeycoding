(ns monkeycoding.editor.codemirror.player
    (:require
      [reagent.core :as r :refer [atom]]
      [monkeycoding.editor.codemirror.common :refer [as-component default-config]]
      [monkeycoding.player :refer [Player]]))


(defn init-player! [dom config paused playback]
  (let [player (new Player dom (clj->js config))]
    (when playback (.play player (clj->js playback)))
    (when paused   (.pause player))
    player))


;; Player is just a nice React wrapper of the JS player
(defn codemirror-player [{:keys [paused playback]}]
  (let [
        pl (atom nil)
        config default-config]

    (as-component {
                    :on-mount (fn [this] (reset! pl (init-player! (r/dom-node this) config paused playback)))
                    :on-props (fn [{paused :paused}] (if paused (.pause @pl) (.resume @pl)))
                    :render (fn [] [:div.player-content])})))
