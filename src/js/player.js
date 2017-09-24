goog.provide("monkeycoding.player");
goog.require("cljsjs.codemirror");


const ZeroPosition = Object.freeze({line : 0, ch : 0});
const EmptySelection = Object.freeze({from :ZeroPosition, to: ZeroPosition });


function initCodemirror(dom, {theme, language, lineNumbers = true, customClassName = null}) {
    let cm = new CodeMirror(dom, {theme, language, lineNumbers, readOnly : true});

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


function positionFromTextOffset(cm, {line, ch}, offset) {
    if (!offset)
      return null;

    let pos  = cm.indexFromPos({line, ch});
    let text   = cm.getValue();

    for (let i = 0; i < offset; i++)
      (text[pos+i] === '\n') ? line++ : ch++;

    return {line, ch};
}


const commands = {

    input(codemirror, {insert = "", remove = 0, position}) {
      let to = positionFromTextOffset(codemirror, position, remove);

      if (to)
        codemirror.replaceRange(insert , position, to);
      else
        codemirror.replaceRange(insert , position);

    },

    selection(codemirror, action) {
      codemirror.setSelection(action.from, action.to);
    },

    cursor(codemirror, action) {
      codemirror.setCursor(action.position);
    },

    mark(codemirror, action) {
      let className = ["highliting-mark", " ", "highliting-mark-id-", action.id].join("");
      codemirror.markText(action.from, action.to, {className, startStyle : "highliting-mark-start"});
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
