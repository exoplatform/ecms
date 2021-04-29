const path = require("path");

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
