const path = require("path");
const ExtractTextWebpackPlugin = require("extract-text-webpack-plugin");

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
  },
  plugins: [
    // we use ExtractTextWebpackPlugin to extract the css code on a css file
    new ExtractTextWebpackPlugin("css/main.css")
  ]
};

module.exports = config;