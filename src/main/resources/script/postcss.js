import postcss from 'https://esm.sh/postcss@8.4.47?dev'
import autoprefixer from 'https://esm.sh/autoprefixer@10.4.14?dev'
import postcssAspectRatio from 'https://esm.sh/postcss-aspect-ratio@1.0.2?dev'

console.log(postcss)

const processer = postcss([autoprefixer(), postcssAspectRatio()])

console.log(processer)

const result = await processer.process('.testclass { aspect-ratio: 16 / 9 }', {from: undefined})

console.log(result.css)