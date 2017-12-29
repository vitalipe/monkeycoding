(ns monkeycoding.editor.player
    (:require
      [reagent.core :as r :refer [atom]]
      [monkeycoding.widgets.util :refer [as-component]]
      [monkeycoding.player :as player]))


(defn player-config->js [{:keys [show-line-numbers theme language]}]
  (->> {"showLineNumbers" show-line-numbers
        "theme" theme
        "language" language}
    (remove (comp nil? second))
    (flatten)
    (apply js-obj)))


(defn compare-and-set-config! [player old-config new-config]
  (when-let [changed (second (clojure.data/diff old-config new-config))]
    (.setConfig player (player-config->js changed))))



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
                    :on-props (fn [{paused :paused config :config}]
                                  (if paused
                                    (.pause @pl)
                                    (.resume @pl)))

                    :on-unmount (fn [_] (.pause @pl))
                    :render (fn [] [:div.player-content {:style {:height "100%"}}])})))


(defn preview-player [{:keys [playback config]}]
  (let [
        pl (atom nil)
        last-config (atom config)]
    (as-component {
                    :on-mount (fn [this] (reset! pl (init-player! (r/dom-node this) true playback #() #() config)))
                    :on-props (fn [{playback :playback config :config}]
                                (doto @pl
                                  (.play playback (clj->js playback))
                                  (.pause))

                                (compare-and-set-config! @pl @last-config config)
                                (reset! last-config config))

                    :render (fn [] [:div.player-content {:style {:height "100%"}}])})))
