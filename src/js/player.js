goog.provide("monkeycoding.player");
goog.require("cljsjs.codemirror");

// TODO: clean up


function initCodemirror(dom, config, initial) {

  delete config.paused;
  delete config.playback;

  var cm = new CodeMirror(dom, config);

  // set initial states
  cm.setValue(initial.snapshot || "");
  cm.setCursor(initial.at || {line: 0, ch: 0});

  // always display cursor
  dom.classList.add("CodeMirror-focused")

  return cm;
}


function omitInBetween(str, from, to) {
  return str.substring(0, from)  + str.substring(to);
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
    _paused = false;

    var inputAction = function() {
      if(_paused)
        return;


      var action     = _playback.inputs[_pos.input];
      var nextAction = _playback.inputs[_pos.input+1];
      var index      = 0;

      console.log("input", action);

      switch (action.type) {
        case "input":
            _cm.replaceRange(action.text, action.at);
            break;

        case "cursor":
            _cm.setCursor(action.at);
            break;

        case "delete":
          index = _cm.indexFromPos(action.at);
          _cm.setValue(omitInBetween(_cm.getValue(), index, index  + action.len));
          break;
      }

      _pos.input++;

      if (nextAction)
        if (nextAction.dt < 10)
          inputAction();
        else
          setTimeout(inputAction, Math.max(nextAction.dt - 40, 0));
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
