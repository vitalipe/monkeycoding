(ns monkeycoding.widgets.icon)


(defn icon [icon-name]
  (->> (case icon-name
          :export      "android-share-alt"
          :commit      "merge"
          :redo        "forward"
          :undo        "reply"
          :record      "record"
          :recording   "ios-albums"
          :menu        "navicon"
          :play        "play"
          :pause       "ios-pause"
          :goto-next   "play.adjust-smaller"
          :goto-prv    "play.horizontal-flip.adjust-smaller"
          :goto-start  "ios-skipbackward"
          :goto-end    "ios-skipforward"
          :timeline    "ios-pulse-strong"
          :close       "close"
          :add-mark    "code"
          :marks       "code-working"
          :delta-time  "android-stopwatch"
          :squash      "arrow-shrink"
          :arrow-down  "ios-arrow-down"
          :checked     "checkmark"
          :settings    "wrench"
          :about       "ios-heart"
          icon-name)

    (str ".icon" ".ion-")
    (str "i." "icon-" (name icon-name))
    (keyword)
    (vector)))
