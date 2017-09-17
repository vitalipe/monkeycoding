(ns monkeycoding.editor.codemirror.editor
    (:require
      [reagent.core :as r :refer [atom]]
      [cljsjs.codemirror]
      [cljsjs.codemirror.mode.javascript]

      [monkeycoding.editor.codemirror.common :refer [default-config as-component]]
      [monkeycoding.editor.codemirror.modes  :as modes]))


(defn- create-codemirror! [dom-node config]
  (new  js/CodeMirror dom-node (clj->js (merge default-config config))))


(defn- init-codemirror-events! [codemirror mode-state-ref]
  (let [dispatch! #(reset! mode-state-ref (%1 @mode-state-ref codemirror %2))]

    (doto (.. codemirror getWrapperElement)
      (.addEventListener "touchstart" #(dispatch! modes/process-dom-event %))
      (.addEventListener "touchend"   #(dispatch! modes/process-dom-event %))
      (.addEventListener "mousedown"  #(dispatch! modes/process-dom-event %))
      (.addEventListener "mouseup"    #(dispatch! modes/process-dom-event %)))

    (doto codemirror
      (.on "change"                #(dispatch! modes/process-codemirror-event %2))
      (.on "cursorActivity"        #(dispatch! modes/process-codemirror-event (.getCursor %)))
      (.on "beforeSelectionChange" #(dispatch! modes/process-codemirror-event %2)))))


(defn- props->mode [{:keys [recording-highlight read-only on-input]}]
  (cond
    recording-highlight (modes/all :highlighting)
    read-only           (modes/all :view-only)
    on-input            (modes/all :recording)

    :default-to         (modes/all :view-only)))


(defn- same-type? [a b] (= (type a) (type b)))


(defn- sync-mode-with-props! [mode-state-ref props cm]
  (let [
        from @mode-state-ref
        to   (props->mode props)]
    (cond
        (same-type? from to) (reset! mode-state-ref (modes/sync-with-props! from cm props))
        :otherwise
                  (do
                      (modes/exit! from cm)
                      (reset! mode-state-ref (modes/enter! to cm props))))))


;; Editor
(defn codemirror-editor [{:keys [
                                  text

                                  read-only
                                  recording-highlight

                                  on-input
                                  on-highlight] :as props}]
  (let [
        cm         (atom nil)
        mode-state (atom (modes/all :uninitialized))]

      (as-component {
                      :on-mount (fn [this]
                                  (do
                                    (reset! cm
                                      (-> this
                                        (r/dom-node)
                                        (create-codemirror! {:value (or text "")})
                                        (init-codemirror-events! mode-state)))

                                    (sync-mode-with-props! mode-state props @cm)))

                      :on-props (fn [{text :text :as props}]
                                  (sync-mode-with-props! mode-state props @cm))

                      :render (fn [] [:div])})))
