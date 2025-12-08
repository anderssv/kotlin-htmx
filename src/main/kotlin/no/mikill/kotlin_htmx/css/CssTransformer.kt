package no.mikill.kotlin_htmx.css

/**
 * Interface for CSS transformers.
 *
 * Implementations can use different CSS processing engines:
 * - [PostCssTransformer]: Uses PostCSS via GraalJS (slower, more plugins)
 * - [LightningCssTransformer]: Uses native LightningCSS binary (faster, simpler)
 */
interface CssTransformer {
    /**
     * Process CSS input through the transformer's pipeline.
     *
     * @param cssContent Raw CSS content to process
     * @return Processed CSS with transformations applied
     * @throws RuntimeException If CSS processing fails
     */
    fun process(cssContent: String): String
}
