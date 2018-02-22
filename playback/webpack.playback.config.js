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
              presets: PROD_BUILD
                        ? ["babel-preset-minify", "babel-preset-env"]
                        : ["babel-preset-env"]
}}}]}}};
