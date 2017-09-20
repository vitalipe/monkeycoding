goog.provide("monkeycoding.player");
goog.require("cljsjs.codemirror");

// TODO: rewirte, fix timing and optimize


function initCodemirror(dom, config, initial) {

  delete config.paused;
  delete config.playback;

  config.readOnly = true;

  var cm = new CodeMirror(dom, config);

  // set initial states
  cm.setValue(initial.snapshot || "");
  cm.setCursor(initial.at || {line: 0, ch: 0});

  // always display cursor
  dom.classList.add("CodeMirror-focused")

  return cm;
}


function omitRange(str, from, to) {
  return str.substring(0, from)  + str.substring(to);
}

function insertAtIndex(str, from, text) {
  return str.substring(0, from)  + text + str.substring(from);
}




/**
 * @constructor
 */
function Player(dom, config) {
  config = (config || {});

  var _paused = Boolean(config.paused);
  var _playback = config.playback || {};
  var _cm = initCodemirror(dom, config, _playback.initial);
  var _pos = {input: 0};


  var _resume = function() {
    if (_pos.input > _playback.inputs.length -1)
      return;

    _paused = false;

    var inputAction = function() {
      if(_paused)
        return;


      var action     = _playback.inputs[_pos.input];
      var nextAction = _playback.inputs[_pos.input+1];
      var index      = 0;
      var text       = "";

      console.log("input", action);

      switch (action.type) {

        case "input":
            index = _cm.indexFromPos(action.position);
            text  = _cm.getValue();

            if (action.remove)
              text = omitRange(text, index, index  + action.remove)

            if (action.insert.length)
              text = insertAtIndex(text, index, action.insert)

              _cm.setValue(text);

        break;

        case "selection":
              _cm.setSelection(action.from, action.to);
        break;

        case "cursor":
            _cm.setCursor(action.position);
        break;

      }

      _pos.input++;

      if (nextAction)
        if (nextAction.dt < 10)
          inputAction();
        else
          setTimeout(inputAction,
            nextAction.type === "input"
               ? Math.max(nextAction.dt - 40, 0)
               : nextAction.dt);
    }

    setTimeout(inputAction, _playback.inputs[_pos.input].dt);
  }

  var _pause = function() {_paused = true}


  // public
  this.setPaused = function(paused) {
    if (_paused === paused)
      return;

    if (paused)
        _pause();
    else
      _resume();
  };

  // start playback ASAP
  if(!_paused)
      _resume();
}


// export
monkeycoding.player.Player = Player;
