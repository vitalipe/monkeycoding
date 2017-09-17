(ns monkeycoding.editor.codemirror.events)


(defn- extract-position [js-pos]
  {:line (.-line js-pos) :ch (.-ch js-pos)})

(defn cm-input-data->event [data]
  (let [
        input  (js->clj data)
        origin (get input "origin")

        cursor-event? (nil? origin)
        selection-event? (contains? input "ranges")
        text-event? (contains? #{"+input" "+delete" "cut" "paste" "copy" "undo" "redo"} origin)]

      (cond
          selection-event? {
                            :type :selection
                            :head   (extract-position (.-head (first (input "ranges"))))
                            :anchor (extract-position (.-anchor (first (input "ranges"))))}

        cursor-event?  {
                        :type :cursor
                        :position (extract-position data)}

        text-event?    {
                         :type :input
                         :insert (clojure.string/join "\n" (input "text"))
                         :remove (count (clojure.string/join "\n" (input "removed")))
                         :position (extract-position (get input "from"))})))


(defn init! [codemirror dom-handler input-handler]
  (let [
        dom-proxy     #(dom-handler codemirror %)
        input-proxy   #(when-let [e (cm-input-data->event %2)] (input-handler %1 e))]

    (doto (.. codemirror getWrapperElement)
      (.addEventListener "touchstart" dom-proxy)
      (.addEventListener "touchend"   dom-proxy)
      (.addEventListener "mousedown"  dom-proxy)
      (.addEventListener "mouseup"    dom-proxy)
      (.addEventListener "mouseleave" dom-proxy))

    (doto codemirror
      (.on "change"                #(input-proxy codemirror %2))
      (.on "cursorActivity"        #(input-proxy codemirror (.getCursor %)))
      (.on "beforeSelectionChange" #(input-proxy codemirror %2)))))
