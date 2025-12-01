/**
 * Webpack Configuration for PostCSS GraalJS Bundle
 *
 * This configuration bundles PostCSS and its plugins into a single JavaScript file
 * that can be executed in the GraalJS runtime (a JavaScript engine for the JVM).
 *
 * Why Webpack?
 * ------------
 * PostCSS and its plugins are designed for Node.js and use CommonJS require().
 * GraalJS doesn't have Node.js built-in modules (fs, path, etc.) by default.
 * Webpack solves this by:
 * 1. Bundling all dependencies into a single file
 * 2. Polyfilling Node.js core modules for browser-like environments
 * 3. Exposing the processCss function as a global variable
 *
 * Build Command:
 * --------------
 * cd src/main/resources/postcss && npm run build
 *
 * This generates dist/bundle.js which is loaded by PostCssTransformer.kt
 */
const path = require('path');
const NodePolyfillPlugin = require('node-polyfill-webpack-plugin');

module.exports = {
  // Entry point: our PostCSS runner script
  entry: './postcss-runner.js',

  output: {
    // Output to dist/bundle.js in the same directory
    path: path.resolve(__dirname, 'dist'),
    filename: 'bundle.js',
    library: {
      // Expose exports as a global variable named 'PostCssProcessorGlobal'
      // This allows GraalJS to access it via: context.getBindings("js").getMember("PostCssProcessorGlobal")
      name: 'PostCssProcessorGlobal',
      type: 'var',
    },
  },

  // Target 'web' creates a browser-like bundle, which is closer to GraalJS
  // than 'node' target (which would expect Node.js built-ins to be available)
  target: 'web',

  plugins: [
    // Provides polyfills for Node.js core modules (process, buffer, etc.)
    // Required because PostCSS and plugins use Node.js APIs
    new NodePolyfillPlugin()
  ],

  module: {
    rules: [
      {
        // Handle ES modules (.mjs) and CommonJS (.js) files
        test: /\.m?js$/,
        resolve: {
          // Allow imports without file extensions (e.g., require('./foo') finds foo.js)
          fullySpecified: false,
        },
      },
    ],
  },
};
