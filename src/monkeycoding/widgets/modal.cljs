(ns monkeycoding.widgets.modal
    (:require
      [monkeycoding.widgets.util :refer [element-has-class?]]))



(defn modal [{:keys [class on-close]} & content]
    [:div.modal.modal-bg {:on-click #(when (element-has-class? % "modal-bg") (on-close))}
      [:div.modal-dialog {:class class}
        (apply conj [:div.modal-content] content)]])


(defn modal-header  [& content] (apply conj [:div.modal-header] content))
(defn modal-content [& content]  (apply conj [:div.modal-body] content))
(defn modal-footer  [& content]   (apply conj [:div.modal-footer] content))
