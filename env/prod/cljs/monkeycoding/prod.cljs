(ns monkeycoding.prod
  (:require
    [monkeycoding.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
