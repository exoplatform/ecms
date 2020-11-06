const path = require("path");
const ExtractTextWebpackPlugin = require("extract-text-webpack-plugin");

let config = {
  context: path.resolve(__dirname, "."),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    "clouddrives-app": "./src/main/webapp/vue-app/connectCloudDrive/main.js",
    "cloudStorage": "./src/main/webapp/vue-app/cloudStorage/main.js"
  },
  output: {
    filename: "js/[name].bundle.js",
    libraryTarget: "amd"
  },
  module: {
    rules: [
      {
        test: /\.css$/,
        use: ["vue-style-loader", "css-loader"]
      },
      {
        test: /\.less$/,
        use: ExtractTextWebpackPlugin.extract({
          fallback: "vue-style-loader",
          use: [
            {
              loader: "css-loader",
              options: {
                sourceMap: true
              }
            },
            {
              loader: "less-loader",
              options: {
                sourceMap: true
              }
            }
          ]
        })
      },
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: ["babel-loader", "eslint-loader"]
      },
      {
        test: /\.vue$/,
        use: ["vue-loader", "eslint-loader"]
      }
    ]
  }
};

module.exports = config;
