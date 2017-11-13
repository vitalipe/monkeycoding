goog.provide("monkeycoding.player");
goog.require("cljsjs.codemirror");


const ZeroPosition = Object.freeze({line : 0, ch : 0});
const EmptySelection = Object.freeze({from :ZeroPosition, to: ZeroPosition });
const NullMark = Object.freeze({id :null, info: null, isNull: true});

function noop() {}

function initCodemirror(dom, config) {
    let cm = new CodeMirror(dom, config);

    dom.classList.add("CodeMirror-focused"); // always display cursor
    dom.classList.add("mokey-code-player");

    if (config.customClassName)
      dom.classList.add(config.customClassName);

    return cm;
}

function registerInteractionEvents(codemirror, ...handlers) {
  let dom = codemirror.getWrapperElement();
  let fromXY = ({pageX, pageY}) => codemirror.coordsChar({top: pageY, left : pageX});
  let dispatch = (pos) => handlers.map(h => h(pos));

  dom.addEventListener("touchstart", ({touches : [xy]}) => dispatch(fromXY(xy)));
  dom.addEventListener("mousemove", (e) => dispatch(fromXY(e)));
}


function applySnapshot(codemirror, {text = "", marks = {}, selection = EmptySelection}) {
    codemirror.setValue(text);
    codemirror.setSelection(selection.from, selection.to);

    Object.values(marks).forEach(({from, to, id}) => mark(codemirror, from, to, id))
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
    let rows = cm.getValue().substr(pos, offset).split('\n');

    if (!rows.length)
      return {line, ch};

    return {
      line: line + (rows.length-1),
      ch:   rows.length > 1 ? (rows[rows.length-1].length) : ch + rows[0].length};
}

function createInlineMarkNode() {
  let node = document.createElement("div");

  node.classList.add("editor-mark-element");
  node.classList.add("CodeMirror-activeline-background");

  return node;
}

function extractMarkID(className) {
  let prefix = "highliting-mark-id-"
  let index = className.indexOf(prefix);

  if(index === -1)
    return null;

  return parseInt(className.substring(index + prefix.length));
}


function sortMarksByPosition(marks) {
  let cmp = (a, b) => ((a > b) ? 1 : ((a === b) ? 0 : -1));
  let cmpPos = (a, b) => (a.line === b.line) ? cmp(a.ch, b.ch) : cmp(a.line, b.line);

  return marks
            .map((m) => Object.assign(m.find(), {id : extractMarkID(m.className)}))
            .sort((a, b) => cmpPos(a.from, b.from));
}


function findMarkAt(sortedMarks, pos) {
  if (pos.outside)
    return NullMark;

  let isAfter  = (before, after) =>
                      (before.line === after.line)
                        ? (before.ch <= after.ch)
                        : (before.line <= before.line);

  let isInsideMark = ({from, to}) => isAfter(from, pos) && !isAfter(to, pos);
  let isAfterMark  = ({from}) => isAfter(from, pos);
  let found = NullMark;

  for (let mark of sortedMarks)
    if      (isInsideMark(mark))  found = mark;
    else if (!isAfterMark(mark))  break;

  return found;
}


function mark(cm, from, to, id) {
    cm.markText(from, to,
                {
                  className : ["highliting-mark", " ", "highliting-mark-id-", id].join(""),
                  startStyle : "highliting-mark-start"});
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
    }
};



class Player {

    constructor(domNode, {
                          highlightActiveLine = true,
                          HighlightActiveMark = true,
                          rawConfig = {},
                          showLineNumbers = true,
                          theme = "",
                          language = "c"}) {
      let config = {
        mode : language,
        theme,
        lineNumbers : showLineNumbers,
        readOnly : true,
        customClassName : null
      };

      this._codemirror = initCodemirror(domNode, Object.assign(rawConfig, config));

      registerInteractionEvents(
          this._codemirror,
          (p) => this._highlightLine(p),
          (p) => this._highlightMark(p));

      this._markNode = createInlineMarkNode();

      // config
      this._isHighlightActiveMark = HighlightActiveMark;
      this._isHighlightActiveLine = highlightActiveLine;


      // stream
      this._stream     = null;
      this._position   = 0;
      this._cancelNext = null;

      this._marksInfo = {};
      this._sortedMarks = [];
      this._lastActiveMark = NullMark;
      this._markLineWidget = null;

      this._lastActiveLine = null;

      // callbacks
      this._markInsertHandler    = noop;
      this._progressHandler      = noop;
      this._markHighlightHandler = noop;
      this._lineHighlightHandler = noop;
    }

    play({initial, inputs, marksInfo}) {
      this.pause();

      this._stream     = inputs;
      this._position   = 0;
      this._cancelNext = null;

      this._marksInfo = marksInfo || {};
      this._sortedMarks = [];
      this._lastActiveMark = NullMark;
      this._markLineWidget = null;

      this._lastActiveLine = null;


      applySnapshot(this._codemirror, initial);

      this._syncMarksCacheInfo();
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
      this._notifyActionExec(nextAction);
    }

    previousStep() {
      let prvAction = this._stream[this._position-2];

      if (!prvAction)
        return;

      this.pause();

      this._execAction(prvAction);
      this._position--;
      this._notifyActionExec(prvAction);
    }


    onMarkInsert(callback)     { this._markInsertHandler    = (callback || noop)}
    onMarkHighlight(callback)  { this._markHighlightHandler = (callback || noop)}
    onLineHighlight(callback)   { this._lineHighlightHandler  = (callback || noop)}
    onProgressUpdate(callback) { this._progressHandler      = (callback || noop)}

    // private
    _highlightLine({line, outside}) {
      let cm = this._codemirror;
      let last = this._lastActiveLine;
      let active = outside ? null : line;

      this._lastActiveLine = active;

      if (line === last)
        return;

      for (let i = cm.lineCount() - 1; i >= 0; i--) // just to be on the safe side
        this._codemirror.removeLineClass(i, "", "CodeMirror-activeline-background");

      if (active === null)
        this._lineHighlightHandler({line : null, text : null});
      else
        this._lineHighlightHandler({line : active, text : this._codemirror.getLine(active)});

      if (active !== null && this._isHighlightActiveLine)
        this._codemirror.addLineClass(line, "", "CodeMirror-activeline-background");
    }

    _highlightMark(pos) {
      let widget = this._markLineWidget;
      let node = this._markNode
      let last = this._lastActiveMark;
      let mark = findMarkAt(this._sortedMarks, pos);
      let handler = this._markHighlightHandler;
      let info = mark.isNull ? null : this._marksInfo[mark.id];

      if (mark.id === last.id)
        return;

      this._markLineWidget = null;
      this._lastActiveMark = mark;

      if (widget)
        widget.clear();

      handler({id: mark.id, info});

      if (mark.isNull || !this._isHighlightActiveMark)
        return;

      node.innerHTML = info;
      this._markLineWidget = this._codemirror.addLineWidget(pos.line, node, {above : true});
    }

    _execAction(action) {
      let command = commands[action.type];
      let codemirror = this._codemirror;

      command(codemirror, action);
      (action.marks || []).map(({from, to, id}) => mark(codemirror, from, to, id));
      this._calcMarksCacheInfo(action);
    }

    _syncMarksCacheInfo() {
      this._sortedMarks = sortMarksByPosition(this._codemirror.getAllMarks());
    }

    _calcMarksCacheInfo(action) {
        if (action.type == "input" || action.marks)
          this._syncMarksCacheInfo()
    }

    _notifyActionExec(action, currentPosition) {
      let total  = this._stream.length;
      let played = this._position; // next index, by starts at 0, so we're fine

      this._progressHandler({total, played});

      if (action.type === "mark")
        this._markInsertHandler({id : action.id, info : action.info})
    }

    _nextTick() {
      let nextAction = this._stream[this._position];
      let onTick = () => {
        this._execAction(nextAction);
        this._position++;
        this._notifyActionExec(nextAction);

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
