goog.provide("monkeycoding.player");
goog.require("cljsjs.codemirror");


const ZeroPosition = Object.freeze({line : 0, ch : 0});
const EmptySelection = Object.freeze({from :ZeroPosition, to: ZeroPosition });


function initCodemirror(dom, {theme, language, showLines = true, customClassName = null}) {
    let cm = new CodeMirror(dom, {theme, language, showLines, readOnly : true});

    dom.classList.add("CodeMirror-focused"); // always display cursor
    dom.classList.add("mokey-code-player");

    if (customClassName)
      dom.classList.add(customClassName);

    return cm;
}


function applySnapshot(codemirror, {text = "", marks = {}, selection = EmptySelection}) {
    codemirror.setValue(text);
    codemirror.setSelection(selection.from, selection.to);
    //FIXME set marks
}


function nextAnimationTick(deltaMs, callback, ...args) {
    let targetTime = (performance.now() + deltaMs - 1000/60);
    let canceled = false;
    let tick = (time) => !canceled &&
                            (time > targetTime)
                              ? callback(...args)
                              : requestAnimationFrame(tick)


    if (deltaMs > 100) // save some battery on long gaps
      setTimeout(tick ,deltaMs - 100/2)
    else
      requestAnimationFrame(tick);

    return () => canceled = true;
}


function omitRange(str, from, to) {
  return str.substring(0, from)  + str.substring(to);
}


function insertAtIndex(str, from, text) {
  return str.substring(0, from)  + text + str.substring(from);
}


const commands = {

    input(codemirror, action) {
      let index = codemirror.indexFromPos(action.position);
      let text  = codemirror.getValue();

      if (action.remove)
        text = omitRange(text, index, index  + action.remove)

      if (action.insert.length)
        text = insertAtIndex(text, index, action.insert)

        codemirror.setValue(text);
    },

    selection(codemirror, action) {
      codemirror.setSelection(action.from, action.to);
    },

    cursor(codemirror, action) {
      codemirror.setCursor(action.position);
    }
};



class Player {

    constructor(domNode, {
                          highlightActiveRow = true,
                          HighlightActiveMark = true,
                          showLineNumbers = true,
                          theme = "",
                          language = "c"}) {

      this._codemirror = initCodemirror(domNode, {language, theme});

      this._stream     = null;
      this._position   = 0;
      this._cancelNext = null;
    }

    play({initial, inputs}) {
      this._stream = inputs;
      this._position = 0;

      applySnapshot(this._codemirror, initial);
      this._nextTick();
    }

    pause() {
      if (!this._cancelNext)
        return this;

      this._cancelNext();
      this._cancelNext = null;

      return this;
    }

    resume() {
      if (this._cancelNext)
        return this;

      this._nextTick();
    }

    nextStep() {
      let nextAction = this._stream[this._position];

      if (!nextAction)
        return;

      this.pause();

      this._execAction(nextAction);
      this._position++;
    }

    previousStep() {
      let prvAction = this._stream[this._position-2];

      if (!prvAction)
        return;

      this.pause();

      this._execAction(prvAction);
      this._position -= 2;
    }


    onMarkInsert() {}
    onMarkHighlight() {}
    onRowHighlight() {}

    onProgressUpdate() {}


    _execAction(action) {
      let command = commands[action.type];
      let codemirror = this._codemirror;

      command(codemirror, action);
    }

    _nextTick() {

      let nextAction = this._stream[this._position];
      let onTick = () => {
        this._execAction(nextAction);
        this._position++;
        this._nextTick();
      }

      if (nextAction)
        this._cancelNext = nextAnimationTick(nextAction.dt, onTick);
      else
        this._cancelNext = null;
    }
}



// export
monkeycoding.player.Player = Player;
