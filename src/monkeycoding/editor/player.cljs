(ns monkeycoding.editor.player
    (:require
      [reagent.core :as r :refer [atom]]
      [monkeycoding.util :refer [as-component]]
      [monkeycoding.player :as player]))


(defn player-config->js [config]
  (js-obj
    "showLineNumbers" (:show-line-numbers config)
    "theme" (:theme config)
    "language" (:language config)
    "rawConfig" (js-obj
                      "scrollbarStyle" "overlay"
                      "coverGutterNextToScrollbar" true)))


(defn init-player! [dom paused playback on-progress on-done config]
  (let [player (new player/Player dom (player-config->js config))]
    (when playback (.play player (clj->js playback)))
    (when paused   (.pause player))
    (.onProgressUpdate player #(do
                                  (on-progress (dec (.-played %)))
                                  (when (= (.-played %) (.-total %)) (on-done))))
    player))



;; player is just a nice React wrapper of the JS player
(defn player [{:keys [
                      paused
                      playback
                      config

                      on-progress
                      on-done]}]

  (let [pl (atom nil)]
    (as-component {
                    :on-mount (fn [this]
                                (reset! pl (init-player!
                                                        (r/dom-node this)
                                                        paused
                                                        playback
                                                        on-progress
                                                        on-done
                                                        config)))
                    :on-props (fn [{paused :paused}]
                                  (if paused
                                    (.pause @pl)
                                    (.resume @pl)))

                    :on-unmount (fn [_] (.pause @pl))
                    :render (fn [] [:div.player-content {:style {:height "100%"}}])})))


(defn preview-player [{:keys [playback config]}]
  (let [pl (atom nil)]
    (as-component {
                    :on-mount (fn [this] (reset! pl (init-player! (r/dom-node this) true playback #() #() config)))
                    :on-props (fn [{playback :playback}]
                                (doto @pl
                                  (.play playback (clj->js playback))
                                  (.pause)))

                    :render (fn [] [:div.player-content {:style {:height "100%"}}])})))
