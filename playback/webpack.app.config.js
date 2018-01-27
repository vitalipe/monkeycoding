// build file for the embedded player

module.exports = (env = {production : false}) => {

  const DESTINATION_ROOT    = (__dirname + "/../public/js/");
  const FILE_NAME           = "monkey-codemirror-player.js";
  const EXPORTED_CLASS_NAME = "MonkeyPlayer";
  const PROD_BUILD          = (env.production === true);


  return {
    entry: { "main": "./codemirror-player.js" },
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
              // using "@babel/preset-env" like it's written in the readme givens an error..
              // I bet it's a webpack bug, anyway using a rel path seems to work
              presets: PROD_BUILD
                        ? ["./node_modules/babel-preset-minify", "./node_modules/babel-preset-env"]
                        : ["./node_modules/babel-preset-env"]
}}}]}}};
