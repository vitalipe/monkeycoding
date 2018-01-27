// build file for the exported playback environment


module.exports = (env = {production : false}) => {

  const VERSION             = [0,0,1];
  const DESTINATION_ROOT    = (__dirname + "/../public/playback/");
  const PROD_BUILD          = (env.production === true);
  const FILE_NAME           = "codemirror-playback." +
                                                      VERSION.join(".") +
                                                      (PROD_BUILD ? ".min" : "") + ".js";


  return {
    entry: { "codemirror": "./src/main.js" },
    output: {
          path: DESTINATION_ROOT,
          filename: "./" + FILE_NAME},

    externals: [{"window": "window"}],

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
                        ? ["../node_modules/babel-preset-minify", "../node_modules/babel-preset-env"]
                        : ["../node_modules/babel-preset-env"]
}}}]}}};
