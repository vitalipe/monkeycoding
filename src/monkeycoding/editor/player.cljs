(ns monkeycoding.editor.player
    (:require
      [reagent.core                            :as r       :refer [atom]]
      [monkeycoding.editor.stream              :as stream  :refer [stream->playback-snapshot stream->playback]]
      [monkeycoding.widgets.util                           :refer [as-component]]
      [clojure.data                            :as data]))


(defn player-config->js [{:keys [show-line-numbers theme language playback-speed]}]
  (->> {"showLineNumbers" show-line-numbers
        "theme" theme
        "language" language
        "playbackSpeed" playback-speed}
    (remove (comp nil? second))
    (flatten)
    (apply js-obj)))


(defn compare-and-set-config! [player old-config new-config]
  (when-let [changed (second (data/diff old-config new-config))]
    (.setConfig player (player-config->js changed))))


(defn init-player! [dom paused playback on-progress on-done config]
  (let [player (new js/MonkeyPlayer dom (player-config->js config))]
    (when playback (.play player (clj->js playback)))
    (when paused   (.pause player))
    (.onProgressUpdate player #(do
                                  (on-progress (dec (.-played %)))
                                  (when (= (.-played %) (.-total %)) (on-done))))
    player))



;; player is just a nice React wrapper of the JS player
(defn player [{:keys [
                      paused
                      recording
                      position
                      config

                      on-progress
                      on-done]}]

  (let [
        pl (atom nil)
        last-config (atom config)]
    (as-component {
                    :on-mount (fn [this]
                                (reset! pl (init-player!
                                                        (r/dom-node this)
                                                        paused
                                                        (stream->playback recording position)
                                                        #(on-progress (+ 1 position %))
                                                        #(on-done) ;; strip args
                                                        config)))
                    :on-props (fn [{paused :paused config :config}]
                                  (if paused
                                    (.pause @pl)
                                    (.resume @pl))

                                  (compare-and-set-config! @pl @last-config config)
                                  (reset! last-config config))

                    :on-unmount (fn [_] (.pause @pl))
                    :render (fn [] [:div.player-content {:style {:height "100%"}}])})))


(defn preview-player [{:keys [recording position config]}]
  (let [
        pl (atom nil)
        last-config (atom config)]
    (as-component {
                    :on-mount (fn [this]
                                (reset! pl
                                        (init-player!
                                          (r/dom-node this)
                                          true
                                          (stream->playback-snapshot recording position)
                                          #() #() config)))
                    :on-props (fn [{:keys [position config recording]}]
                                (let [playback (stream->playback-snapshot recording position)]
                                  (doto @pl
                                    (.play playback (clj->js config))
                                    (.pause)))

                                (compare-and-set-config! @pl @last-config config)
                                (reset! last-config config))

                    :render (fn [] [:div.player-content {:style {:height "100%"}}])})))
