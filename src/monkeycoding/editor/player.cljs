(ns monkeycoding.editor.player
    (:require
      [reagent.core :as r :refer [atom]]
      [monkeycoding.editor.common :refer [default-config]]
      [monkeycoding.util :refer [as-component]]
      [monkeycoding.player :as player]))


(def config
        (js-obj
          "showLineNumbers" (:show-line-numbers default-config)
          "theme" (:theme default-config)
          "language" (:language default-config)
          "rawConfig" (js-obj
                            "scrollbarStyle" "overlay"
                            "coverGutterNextToScrollbar" true)))


(defn init-player! [dom paused playback on-progress]
  (let [player (new player/Player dom (clj->js config))]
    (when playback (.play player (clj->js playback)))
    (when paused   (.pause player))
    (.onProgressUpdate player #(on-progress (dec (.-played %))))

    player))



;; player is just a nice React wrapper of the JS player
(defn player [{:keys [paused playback on-progress]}]
  (let [pl (atom nil)]
    (as-component {
                    :on-mount (fn [this] (reset! pl (init-player! (r/dom-node this) paused playback on-progress)))
                    :on-props (fn [{paused :paused}]
                                  (if paused
                                    (.pause @pl)
                                    (.resume @pl)))

                    :on-unmount (fn [_] (.pause @pl))
                    :render (fn [] [:div.player-content {:style {:height "100%"}}])})))


(defn preview-player [{:keys [playback]}]
  (let [pl (atom nil)]
    (as-component {
                    :on-mount (fn [this] (reset! pl (init-player! (r/dom-node this) true playback #())))
                    :on-props (fn [{playback :playback}]
                                (doto @pl
                                  (.play playback (clj->js playback))
                                  (.pause)))

                    :render (fn [] [:div.player-content {:style {:height "100%"}}])})))
