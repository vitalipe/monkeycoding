(ns monkeycoding.editor.codemirror.exporter
  (:require
    [monkeycoding.editor.stream     :as stream]
    [monkeycoding.editor.player     :refer [player-config->js]]))



(def default-options
    {:show-line-numbers true
     :show-hightlights true
     :language        "clojure"
     :theme           "seti"
     :playback-speed  1
     :auto-play false
     :parent-selector "#id-1337"})


(def codemirror-cdn "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.24.0")
(def player-cdn     "https://127.0.0.1/dist")


(defn- css-tag [& path]
  (str "<link href=\"" (clojure.string/join "/" path) ".min.css\" rel=\"stylesheet\" type=\"text/css\">"))

(defn- script-tag [& path]
  (str "<script src=\"" (clojure.string/join "/" path) ".min.js\" ></script>"))


(defn compile-dependecies [options]
  (clojure.string/join "\n"
    [
      (script-tag player-cdn "0.0.1" "player")
      (css-tag codemirror-cdn "codemirror")
      (css-tag codemirror-cdn "theme/" (:theme options))]))



(defn compile-playback [{:keys [parent-selector auto-play] :as options} recording]
  (let [
        playback (.stringify js/JSON (stream/stream->playback recording))
        player-config (.stringify js/JSON (player-config->js options))]
    (str
      "(function(MonkeyCoding) { \n"
      "var node = document.querySelector(" "\"" parent-selector "\"" ")); \n"
      "var Player = MonkeyCoding.latestPlayerByVersion(" 0 "," 0 "," 1 ", \"codemirror\"); \n"
      "var playback = Player.createPlayback(node, \n"
                                                  "// player config: \n"
                                                  player-config ", \n"
                                                  "// playback: \n"
                                                  playback "\n"
                                                  "," auto-play "); \n"
      "\n"
      "MonkeyCoding.playbacks[" 42 "] = playback); \n"
      "return playback; \n"
      "\n"
      "})(MonkeyCoding);")))
