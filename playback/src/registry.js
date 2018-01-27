const window = require("window");

// like clojure's "get-in"
function _get(obj, path) {
  if (path.length === 0 || !obj)
    return obj;

  var next = obj[path[0]];

  path.unshift();
  return _get(next, path);
}

// like clojure's "assoc-in"
function _set(obj, path, value) {
    var next = path[0];

    if (!obj[next])
      obj[next] = {};

    if (path.length === 1)
      return obj[next] = value;

    path.shift();
    _set(obj[next], path, value);
}

let store = {};

// because this code will be shared between multiple instances we will publish
// explicitly to window, but first we want to make sure that we don't override
if (!window.MonkeyCoding)
  window.MonkeyCoding = {
                          _registry : store,
                          find      : _get.bind(null, store),
                          register  : _set.bind(null, store)};

// so that we can require it
module.exports = window.MonkeyCoding;
