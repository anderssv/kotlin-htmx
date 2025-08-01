const path = require('path'); // Node.js path module, available in this script
const NodePolyfillPlugin = require('node-polyfill-webpack-plugin');

module.exports = {
  entry: './postcss-runner.js', // Our existing runner script
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'bundle.js',
    library: {
      name: 'PostCssProcessorGlobal', // Global variable name
      type: 'var', // Expose as a global variable
    },
  },
  target: 'web', // Target a browser-like environment, which is closer to GraalJS without Node.js compat
  plugins: [
    new NodePolyfillPlugin() // Automatically provides polyfills for Node core modules
  ],
  module: {
    rules: [
      {
        test: /\.m?js$/,
        resolve: {
          fullySpecified: false, // Allows imports without file extensions
        },
      },
    ],
  },
};
