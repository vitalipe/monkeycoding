const registry = require("./registry");

const CodeMirrorPlayer = require("./codemirror-player");
const Toolbar = require("./toolbar");
const Playback = require("./playback");


registry.register([0,0,1, "codemirror-export", "player"], CodeMirrorPlayer);
registry.register([0,0,1, "common", "toolbar"], Toolbar);
registry.register([0,0,1, "common", "playback"], Playback);
