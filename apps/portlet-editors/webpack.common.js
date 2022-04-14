const path = require("path");
const ESLintPlugin = require('eslint-webpack-plugin');
const { VueLoaderPlugin } = require('vue-loader')

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
  plugins: [
    new ESLintPlugin({
      files: [
        './src/main/webapp/vue-apps/*.js',
        './src/main/webapp/vue-apps/*.vue',
        './src/main/webapp/vue-apps/**/*.js',
        './src/main/webapp/vue-apps/**/*.vue',
      ],
    }),
    new VueLoaderPlugin()
  ],
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          "babel-loader",
        ]
      },
      {
        test: /\.vue$/,
        use: [
          "vue-loader",
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