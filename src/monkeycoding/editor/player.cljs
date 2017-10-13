(ns monkeycoding.editor.player
    (:require
      [reagent.core :as r :refer [atom]]
      [monkeycoding.editor.common :refer [as-component default-config]]
      [monkeycoding.player :as player]))


(defn init-player! [dom config paused playback]
  (let [player (new player/Player dom (clj->js config))]
    (when playback (.play player (clj->js playback)))
    (when paused   (.pause player))
    player))


;; Player is just a nice React wrapper of the JS player
(defn player [{:keys [paused playback]}]
  (let [
        pl (atom nil)
        config (js-obj
                  "showLineNumbers" (:show-line-numbers default-config)
                  "theme" (:theme default-config)
                  "language" (:language default-config)
                  "rawConfig" (js-obj
                                    "scrollbarStyle" "overlay"
                                    "coverGutterNextToScrollbar" true))]



    (as-component {
                    :on-mount (fn [this] (reset! pl (init-player! (r/dom-node this) config paused playback)))
                    :on-props (fn [{paused :paused}] (if paused (.pause @pl) (.resume @pl)))
                    :render (fn [] [:div.player-content {:style {:height "100%"}}])})))
