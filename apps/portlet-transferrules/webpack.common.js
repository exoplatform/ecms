const path = require("path");
const ExtractTextWebpackPlugin = require("extract-text-webpack-plugin");

let config = {
  context: path.resolve(__dirname, "."),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    transferRules: "./src/main/webapp/vue-apps/transferRules/main.js"
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
            'babel-loader',
            'eslint-loader',
          ]
        },
        {
          test: /\.vue$/,
          use: [
            'vue-loader',
            'eslint-loader',
          ]
        }
      ]
    }
};

module.exports = config;