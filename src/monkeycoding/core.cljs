(ns monkeycoding.core
    (:require
      [reagent.core :as r]
      [monkeycoding.editor.screen :refer [editor-screen]]))



;; -------------------------
;; Initialize app

(defn mount-root []
  (r/render [editor-screen] (.getElementById js/document "app")))

(defn init! []
  (mount-root))
