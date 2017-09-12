(ns ^:figwheel-no-load monkeycoding.dev
  (:require
    [monkeycoding.core :as core]
    [devtools.core :as devtools]))


(enable-console-print!)

(devtools/install!)

(core/init!)
