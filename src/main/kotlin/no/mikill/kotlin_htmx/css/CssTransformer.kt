package no.mikill.kotlin_htmx.css

/**
 * Interface for CSS transformers.
 *
 * Current implementation:
 * - [LightningCssTransformer]: Uses native LightningCSS binary for fast CSS processing
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
