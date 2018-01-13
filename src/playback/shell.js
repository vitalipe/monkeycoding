
(function(window) {

  if(window["MonkeyCoding"])
    return;

  var _get = function(obj, path) {
    if (path.length === 0 || !obj)
      return obj;

    var next = obj[path[0]];

    path.unshift();
    return _get(next, path);
  }

  var _set = function(obj, path, value) {
      var next = path[0];

      if (!obj[next])
        obj[next] = {};

      if (path.length === 1)
        return obj[next] = value;

      path.unshift();
      _set(obj[next], path, value);
  }


  // export
  window["MonkeyCoding"] = {};
  window["MonkeyCoding"]["find"] = _get.bind(null, window["MonkeyCoding"]);
  window["MonkeyCoding"]["register"] = _set.bind(null, window["MonkeyCoding"]);;

})(window);
