(ns monkeycoding.editor.codemirror.editor
    (:require
      [reagent.core :as r :refer [atom]]
      [cljsjs.codemirror]
      [cljsjs.codemirror.mode.javascript]

      [monkeycoding.editor.codemirror.common  :refer [default-config as-component]]
      [monkeycoding.editor.codemirror.modes   :as modes]
      [monkeycoding.editor.codemirror.events  :as events]))



;; mode management
(defn- props->mode [{:keys [recording-highlight read-only on-input]}]
  (cond
    recording-highlight (modes/all :highlighting)
    read-only           (modes/all :view-only)
    on-input            (modes/all :recording)

    :default-to         (modes/all :view-only)))


(defn- sync-mode-with-props! [mode-state-ref props cm]
  (let [
        from @mode-state-ref
        to   (props->mode props)
        same-mode? (= (type from) (type to))]

    (cond
        same-mode? (reset! mode-state-ref (modes/sync-with-props! from cm props))
        :otherwise
                  (do
                      (modes/exit! from cm)
                      (reset! mode-state-ref (modes/enter! to cm props))))))

(defn- dispatch-dom-event! [mode-state-ref cm event]
  (reset! mode-state-ref (modes/process-dom-event @mode-state-ref cm event)))


(defn- dispatch-codemirror-event! [mode-state-ref cm event]
  (reset! mode-state-ref (modes/process-codemirror-event @mode-state-ref cm event)))


;; Editor
(defn- create-codemirror! [dom-node config]
  (new  js/CodeMirror dom-node (clj->js (merge default-config config))))


(defn- init! [dom mode-state-ref config]
    (-> dom
      (create-codemirror! config)
      (events/init!
                (partial dispatch-dom-event! mode-state-ref)
                (partial dispatch-codemirror-event! mode-state-ref))))


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
                                    (reset! cm (init! (r/dom-node this) mode-state {:value (or text "")}))
                                    (sync-mode-with-props! mode-state props @cm))

                      :on-props (fn [{text :text :as props}]
                                  (sync-mode-with-props! mode-state props @cm))

                      :render (fn [] [:div])})))
