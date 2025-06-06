// Polyfill for `global` if not present
if (typeof global === 'undefined') {
    if (typeof globalThis !== 'undefined') {
        global = globalThis;
    } else if (typeof self !== 'undefined') {
        global = self;
    } else {
        global = this || Function('return this')();
    }
}
// Polyfill for `process` if not present
if (typeof process === 'undefined') {
    process = { env: {} };
}

let postcss;
let autoprefixer;
let postcssCalc;
let postcssSimpleVars;
let postcssNested;

try {
    // console.log('Attempting to require postcss...');
    postcss = require('postcss');
    // console.log('PostCSS loaded successfully.');
} catch (e) {
    // console.error('Failed to require postcss:', e.message, e.stack);
    throw e; // Re-throw to see it in Kotlin
}

try {
    // console.log('Attempting to require autoprefixer...');
    autoprefixer = require('autoprefixer');
    // console.log('Autoprefixer loaded successfully.');
} catch (e) {
    // console.error('Failed to require autoprefixer:', e.message, e.stack);
    throw e; // Re-throw to see it in Kotlin
}

try {
    postcssCalc = require('postcss-calc');
} catch (e) {
    throw e; // Re-throw to see it in Kotlin
}

try {
    postcssSimpleVars = require('postcss-simple-vars');
} catch (e) {
    throw e; // Re-throw to see it in Kotlin
}

try {
    postcssNested = require('postcss-nested');
} catch (e) {
    throw e; // Re-throw to see it in Kotlin
}

function processCss(cssInput) {
    return new Promise((resolve, reject) => {
        try {
            if (!postcss || !autoprefixer || !postcssCalc || !postcssSimpleVars || !postcssNested) {
                reject('One or more PostCSS plugins not loaded');
                return;
            }
            postcss([
                postcssSimpleVars(),
                postcssNested(),
                postcssCalc(),
                autoprefixer
            ])
                .process(cssInput, { from: undefined })
                .then(result => resolve(result.css))
                .catch(error => reject(error.toString()));
        } catch (e) {
            reject(e.toString());
        }
    });
}

module.exports = { processCss };
