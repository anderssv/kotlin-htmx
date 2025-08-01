// Polyfill for `process` if not present
if (typeof process === 'undefined') {
    process = {env: {}};
}

const postcss = require('postcss');
const autoprefixer = require('autoprefixer');
const postcssCalc = require('postcss-calc');
const postcssSimpleVars = require('postcss-simple-vars');
const postcssNested = require('postcss-nested');

function processCss(cssInput) {
    return new Promise((resolve, reject) => {
        try {
            postcss([
                postcssSimpleVars(),
                postcssNested(),
                postcssCalc(),
                autoprefixer
            ])
                .process(cssInput, {from: undefined})
                .then(result => resolve(result.css))
                .catch(error => reject(error.toString()));
        } catch (e) {
            reject(e.toString());
        }
    });
}

module.exports = {processCss};
