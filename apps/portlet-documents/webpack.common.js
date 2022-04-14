const path = require('path');
const ESLintPlugin = require('eslint-webpack-plugin');
const { VueLoaderPlugin } = require('vue-loader')

let config = {
  context: path.resolve(__dirname, '.'),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    commonDocComponents: './src/main/webapp/vue-app/common/main.js',  
    documents: './src/main/webapp/vue-app/documents/main.js',
    filesSearch: './src/main/webapp/vue-app/files-search/main.js',
    attachmentApp: './src/main/webapp/vue-app/attachment/main.js',
    attachmentIntegration: './src/main/webapp/vue-app/attachment-integration/main.js',
    legacyComposerAttachments: './src/main/webapp/vue-app/legacy-composer-attachments/main.js',
  },
  output: {
    filename: 'js/[name].bundle.js',
    libraryTarget: 'amd'
  },
  plugins: [
    new ESLintPlugin({
      files: [
        './src/main/webapp/vue-app/*.js',
        './src/main/webapp/vue-app/*.vue',
        './src/main/webapp/vue-app/**/*.js',
        './src/main/webapp/vue-app/**/*.vue',
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
          'babel-loader',
        ]
      },
      {
        test: /\.vue$/,
        use: [
          'vue-loader',
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