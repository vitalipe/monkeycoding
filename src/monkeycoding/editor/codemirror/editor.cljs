(ns monkeycoding.editor.codemirror.editor
    (:require
      [reagent.core :as r :refer [atom]]
      [cljsjs.codemirror]
      [cljsjs.codemirror.addon.scroll.simplescrollbars]

      [cljsjs.codemirror.mode.javascript]

      [monkeycoding.editor.common             :refer [default-config as-component]]
      [monkeycoding.editor.codemirror.modes   :as modes]
      [monkeycoding.editor.codemirror.parse   :as parse]))



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
                      ;; if we don't reset! before we enter! a new mode
                      ;; the previous mode might get input events...
                      (reset! mode-state-ref (:uninitialized modes/all))
                      (modes/exit! from cm)
                      (reset! mode-state-ref (modes/enter! to cm props))))))


;; event proxy
(defn- dispatch-dom-event! [mode-state-ref cm event]
  (reset! mode-state-ref (modes/process-dom-event @mode-state-ref cm event)))


(defn- dispatch-codemirror-event! [mode-state-ref cm js-event]
  (when-let [event (parse/js->step js-event)]
    (reset! mode-state-ref (modes/process-input-event @mode-state-ref cm event))))


(defn init-events! [codemirror mode-state-ref]
  (let [
        dom-proxy     #(dispatch-dom-event! mode-state-ref codemirror %)
        input-proxy   #(dispatch-codemirror-event! mode-state-ref %1 %2)]

    (doto (.. codemirror getWrapperElement)
      (.addEventListener "touchstart" dom-proxy)
      (.addEventListener "touchend"   dom-proxy)
      (.addEventListener "mousedown"  dom-proxy)
      (.addEventListener "mouseup"    dom-proxy)
      (.addEventListener "mouseleave" dom-proxy)
      (.addEventListener "mousemove"  dom-proxy))

    (doto codemirror
      (.on "change"                input-proxy)
      (.on "cursorActivity"        #(input-proxy % (.getCursor %)))
      (.on "beforeSelectionChange" input-proxy))))


;; Editor
(defn- create-codemirror! [dom-node config]
  (let [config (merge default-config config)]
    (new  js/CodeMirror dom-node (js-obj
                                    "lineNumbers" (:show-line-numbers config)
                                    "theme" (:theme config)
                                    "language" (:language config)
                                    "scrollbarStyle" "overlay"
                                    "coverGutterNextToScrollbar" true))))



(defn- init! [dom mode-state-ref config]
    (set! (.-cm js/window)
          (-> dom
                (create-codemirror! config)
                (init-events! mode-state-ref))))


(defn codemirror-editor [{:keys [
                                  text
                                  selection
                                  marks
                                  dt-cap

                                  read-only
                                  recording-highlight

                                  on-input
                                  on-highlight] :as props}]
  (let [
        cm         (atom nil)
        mode-state (atom (modes/all :uninitialized))]

      (as-component {
                      :on-mount (fn [this]
                                    (reset! cm (init! (r/dom-node this) mode-state {}))
                                    (sync-mode-with-props! mode-state props @cm))

                      :on-props (fn [{text :text :as props}]
                                  (sync-mode-with-props! mode-state props @cm))

                      :render (fn [] [:div {:style {:height "100%"}}])})));
