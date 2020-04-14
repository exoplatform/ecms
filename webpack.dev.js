const path = require("path");
const merge = require("webpack-merge");
const webpackCommonConfig = require("./webpack.common.js");

// the display name of the war
const app = "cloud-drive";

// add the server path to your server location path
// const exoServerPath = "/exo-server";
const exoServerPath = "D:/sasha_work/platform/platform-6.0.0-M27";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${exoServerPath}/webapps/${app}/`),
    filename: "js/[name].bundle.js"
  },
  devtool: "inline-source-map"
});

module.exports = config;
