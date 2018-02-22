// build file for the app embedded player

module.exports = (env = {production : false}) => {

  const DESTINATION_ROOT    = (__dirname + "/../public/js/");
  const FILE_NAME           = "monkey-codemirror-player.js";
  const EXPORTED_CLASS_NAME = "MonkeyPlayer";
  const PROD_BUILD          = (env.production === true);


  return {
    entry: { "main": "./src/codemirror-player.js" },
    output: {
          path: DESTINATION_ROOT,
          filename: "./" + FILE_NAME,
          library: EXPORTED_CLASS_NAME},

    module: {
      rules: [
        {
          test: /\.js$/,
          exclude: /(node_modules)/,
          use: {
            loader: "babel-loader",
            options: {
              presets: PROD_BUILD
                        ? ["babel-preset-minify", "babel-preset-env"]
                        : []
}}}]}}};
