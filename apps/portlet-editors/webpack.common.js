const path = require("path");

let config = {
  context: path.resolve(__dirname, "."),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    editorsAdmin: "./src/main/webapp/vue-apps/editorsAdmin/main.js"
  },
  output: {
    filename: "javascript/[name].bundle.js",
    libraryTarget: "amd"
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          "babel-loader",
          "eslint-loader"
        ]
      },
      {
        test: /\.vue$/,
        use: [
          "vue-loader",
          "eslint-loader"
        ]
      }
    ]
  },
  externals: {
    vue: "Vue",
    vuetify: "Vuetify",
  }
};

module.exports = config;