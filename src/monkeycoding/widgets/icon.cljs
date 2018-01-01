(ns monkeycoding.widgets.icon)


(defn icon [props]
  (if-not (map? props)
    (icon {:icon props})
    (->> (case (:icon props)
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
            :delete      "trash-b"
            (:icon props))
      (str ".icon" ".ion-")
      (str "." (apply str (:class props)))
      (str "i." "icon-" (name (:icon props)))
      (keyword)
      (vector))))
