goog.provide("monkeycoding.playback.codemirror");

goog.require("monkeycoding.playback.codemirror.player");
goog.require("monkeycoding.playback");
goog.require("monkeycoding.playback.shell");


var Player    = monkeycoding.playback.codemirror.player.Player;
var Playback  =  monkeycoding.playback.Playback;

var shell     =  monkeycoding.playback.shell;

shell.register([0,1, "codemirror-export", "player"], Player);
shell.register([0,1, "common", "playback"], Playback);
shell.register([0,1, "common", "test"], 42);



// export
monkeycoding.playback.codemirror = {};
