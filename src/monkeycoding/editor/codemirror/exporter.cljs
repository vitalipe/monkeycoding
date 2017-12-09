(ns monkeycoding.editor.codemirror.exporter
  (:require
    [monkeycoding.editor.stream     :as stream]
    [monkeycoding.editor.player     :refer [player-config->js]]))


;; the following list of themes is genareted by:
;; svn ls https://github.com/codemirror/CodeMirror/trunk/theme | sed -e 's/\..*$//' | while read t
;; do echo ":$t {:display-name \"$t\" :value \"$t\" :path \"theme/$t\"}"; done
(def themes {
              :3024-day {:display-name "3024-day" :value "3024-day"}
              :3024-night {:display-name "3024-night" :value "3024-night"}
              :abcdef {:display-name "abcdef" :value "abcdef"}
              :ambiance {:display-name "ambiance" :value "ambiance"}
              :ambiance-mobile {:display-name "ambiance-mobile" :value "ambiance-mobile"}
              :base16-dark {:display-name "base16-dark" :value "base16-dark"}
              :base16-light {:display-name "base16-light" :value "base16-light"}
              :bespin {:display-name "bespin" :value "bespin"}
              :blackboard {:display-name "blackboard" :value "blackboard"}
              :cobalt {:display-name "cobalt" :value "cobalt"}
              :colorforth {:display-name "colorforth" :value "colorforth"}
              :dracula {:display-name "dracula" :value "dracula"}
              :duotone-dark {:display-name "duotone-dark" :value "duotone-dark"}
              :duotone-light {:display-name "duotone-light" :value "duotone-light"}
              :eclipse {:display-name "eclipse" :value "eclipse"}
              :elegant {:display-name "elegant" :value "elegant"}
              :erlang-dark {:display-name "erlang-dark" :value "erlang-dark"}
              :hopscotch {:display-name "hopscotch" :value "hopscotch"}
              :icecoder {:display-name "icecoder" :value "icecoder"}
              :isotope {:display-name "isotope" :value "isotope"}
              :lesser-dark {:display-name "lesser-dark" :value "lesser-dark"}
              :liquibyte {:display-name "liquibyte" :value "liquibyte"}
              :material {:display-name "material" :value "material"}
              :mbo {:display-name "mbo" :value "mbo"}
              :mdn-like {:display-name "mdn-like" :value "mdn-like"}
              :midnight {:display-name "midnight" :value "midnight"}
              :monokai {:display-name "monokai" :value "monokai"}
              :neat {:display-name "neat" :value "neat"}
              :neo {:display-name "neo" :value "neo"}
              :night {:display-name "night" :value "night"}
              :panda-syntax {:display-name "panda-syntax" :value "panda-syntax"}
              :paraiso-dark {:display-name "paraiso-dark" :value "paraiso-dark"}
              :paraiso-light {:display-name "paraiso-light" :value "paraiso-light"}
              :pastel-on-dark {:display-name "pastel-on-dark" :value "pastel-on-dark"}
              :railscasts {:display-name "railscasts" :value "railscasts"}
              :rubyblue {:display-name "rubyblue" :value "rubyblue"}
              :seti {:display-name "seti" :value "seti"}
              :solarized {:display-name "solarized" :value "solarized"}
              :the-matrix {:display-name "the-matrix" :value "the-matrix"}
              :tomorrow-night-bright {:display-name "tomorrow-night-bright" :value "tomorrow-night-bright"}
              :tomorrow-night-eighties {:display-name "tomorrow-night-eighties" :value "tomorrow-night-eighties"}
              :ttcn {:display-name "ttcn" :value "ttcn"}
              :twilight {:display-name "twilight" :value "twilight"}
              :vibrant-ink {:display-name "vibrant-ink" :value "vibrant-ink"}
              :xq-dark {:display-name "xq-dark" :value "xq-dark"}
              :xq-light {:display-name "xq-light" :value "xq-light"}
              :yeti {:display-name "yeti" :value "yeti"}
              :zenburn {:display-name "zenburn" :value "zenburn"}})




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
