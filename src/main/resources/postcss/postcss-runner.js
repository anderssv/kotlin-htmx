/**
 * PostCSS Runner for GraalJS
 *
 * This module provides CSS processing capabilities using PostCSS and its plugins.
 * It is designed to be bundled with Webpack and executed in a GraalJS context
 * (see PostCssTransformer.kt for the Kotlin integration).
 *
 * Architecture Overview:
 * ----------------------
 * 1. This file is the entry point for Webpack bundling
 * 2. Webpack bundles this + all dependencies into dist/bundle.js
 * 3. The bundle is loaded by GraalJS (a JavaScript engine for the JVM)
 * 4. The processCss function is exposed globally as PostCssProcessorGlobal.processCss
 *
 * Why GraalJS?
 * ------------
 * PostCSS and its plugins are Node.js packages with no pure Java alternatives.
 * GraalJS allows us to run JavaScript on the JVM, enabling server-side CSS
 * processing without spawning external Node.js processes.
 *
 * Plugin Order Matters:
 * --------------------
 * 1. postcssSimpleVars - Variable substitution ($var-name)
 * 2. postcssNested - Flatten nested selectors (SCSS-like nesting)
 * 3. postcssCalc - Resolve calc() expressions where possible
 * 4. autoprefixer - Add vendor prefixes for browser compatibility
 *
 * The order ensures variables are resolved before nesting is processed,
 * and autoprefixer runs last to prefix the final CSS.
 */

// Polyfill for `process` if not present (required for some Node.js packages in browser-like environments)
if (typeof process === 'undefined') {
    process = {env: {}};
}

const postcss = require('postcss');
const autoprefixer = require('autoprefixer');
const postcssCalc = require('postcss-calc');
const postcssSimpleVars = require('postcss-simple-vars');
const postcssNested = require('postcss-nested');

/**
 * Process CSS input through the PostCSS plugin pipeline.
 *
 * @param {string} cssInput - Raw CSS with PostCSS syntax (variables, nesting, etc.)
 * @returns {Promise<string>} - Processed CSS with all transformations applied
 * @throws {Error} - If CSS syntax is invalid or processing fails
 *
 * Example input:
 *   $primary-color: #007bff;
 *   .card {
 *       background: $primary-color;
 *       .title { font-weight: bold; }
 *   }
 *
 * Example output:
 *   .card { background: #007bff; }
 *   .card .title { font-weight: bold; }
 */
function processCss(cssInput) {
    return new Promise((resolve, reject) => {
        try {
            postcss([
                postcssSimpleVars(),  // Resolve $variable references
                postcssNested(),      // Flatten nested selectors
                postcssCalc({}),      // Evaluate calc() expressions
                autoprefixer          // Add vendor prefixes
            ])
                .process(cssInput, {from: undefined})  // 'from: undefined' suppresses source map warnings
                .then(result => resolve(result.css))
                .catch(error => reject(error.toString()));
        } catch (e) {
            reject(e.toString());
        }
    });
}

// Export for Webpack to expose as global variable (see webpack.config.js)
module.exports = {processCss};
