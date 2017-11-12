(ns monkeycoding.editor.codemirror.exporter)


(def default-options
    {:show-line-numbers true
     :show-hightlights true
     :language        "clojure"
     :theme           "seti"
     :playback-speed  1
     :parent-selector "me-1337"})


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



(defn compile-fragment-text [options stream] "")
