(ns monkeycoding.editor.codemirror.highlighter
    (:require
      [reagent.core :as r :refer [atom]]

      [monkeycoding.util                      :refer [as-component]]
      [monkeycoding.editor.codemirror         :refer [create-codemirror!]]
      [monkeycoding.editor.codemirror.parse   :as parse]
      [monkeycoding.editor.codemirror.snapshot   :as snapshot]))


(defn- non-empty-selection? [{:keys [type from to]}]
  (and
    (= type :selection)
    (not= from to)))


(defn- commit-mark [{:keys [from to] :as selection} callback]
  (when (non-empty-selection? selection)
    (cond
      (> (:line from) (:line to)) (callback to from)
      (> (:ch from) (:ch to))     (callback to from)
      :otherwise                  (callback from to))))


(defn process-dom-event [{selection :selection :as state}  on-highlight dom-event]
  (when (contains?  #{"mouseup" "touchend"} (.-type dom-event))
    (commit-mark selection on-highlight))
  (assoc state :selection {}))


(defn process-selection-event [state event]
  (merge state
    (when (non-empty-selection? event) {:selection event})))


(defn init-selection-event! [codemirror callback]
  (doto codemirror
    (.on "beforeSelectionChange" #(callback (parse/js->input %2)))))


(defn init-dom-events! [codemirror callback]
  (doto (.. codemirror getWrapperElement)
    (.addEventListener "touchstart" callback)
    (.addEventListener "touchend"   callback)
    (.addEventListener "mousedown"  callback)
    (.addEventListener "mouseup"    callback)
    (.addEventListener "mouseleave" callback)))


(defn component [{:keys [
                          text
                          selection
                          marks
                          config
                          on-highlight] :as intitial-props}]

  (let [
        cm    (atom nil)
        props (atom intitial-props)
        state (atom {:selection nil})]

      (as-component {
                      :on-mount (fn [this]
                                  (let [
                                        codemirror (create-codemirror! (r/dom-node this) config)
                                        selection-callback #(reset! state (process-selection-event @state %))
                                        dom-callback #(reset! state (process-dom-event @state (:on-highlight @props) %))]

                                      (->> (doto codemirror
                                              (snapshot/apply-snapshot! intitial-props)
                                              (init-selection-event!    selection-callback)
                                              (init-dom-events!         dom-callback))
                                          (reset! cm))))


                      :on-props (fn [new-props] (reset! props new-props))
                      :render (fn [] [:div.editor-mode-highlighting {:style {:height "100%"}}])})));
