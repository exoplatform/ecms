const path = require('path');

let config = {
  context: path.resolve(__dirname, '.'),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    commonDocComponents: './src/main/webapp/vue-app/common/main.js',  
    documents: './src/main/webapp/vue-app/documents/main.js',
    filesSearch: './src/main/webapp/vue-app/files-search/main.js',
    attachmentApp: './src/main/webapp/vue-app/attachment/main.js',
    attachmentIntegration: './src/main/webapp/vue-app/attachment-integration/main.js'
  },
  output: {
    filename: 'js/[name].bundle.js',
    libraryTarget: 'amd'
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          'babel-loader',
          'eslint-loader'
        ]
      },
      {
        test: /\.vue$/,
        use: [
          'vue-loader',
          'eslint-loader'
        ]
      }
    ]
  },
  externals: {
    vue: 'Vue',
    vuetify: 'Vuetify',
  }
};

module.exports = config;