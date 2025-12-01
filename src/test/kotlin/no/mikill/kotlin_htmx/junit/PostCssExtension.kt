package no.mikill.kotlin_htmx.junit

import no.mikill.kotlin_htmx.css.PostCssTransformer
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * JUnit 5 Extension that provides a shared PostCssTransformer across all tests.
 *
 * The PostCssTransformer is expensive to create (~4-8s for GraalVM JS initialization),
 * so we share a single instance across all test classes using this extension.
 *
 * Features:
 * - Creates PostCssTransformer once per JVM (stored in root ExtensionContext.Store)
 * - Injects PostCssTransformer via ParameterResolver
 * - No cleanup needed (GraalVM contexts are daemon threads)
 *
 * Usage:
 * ```
 * @ExtendWith(PostCssExtension::class)
 * class MyCssTest {
 *     @Test
 *     fun myTest(transformer: PostCssTransformer) {
 *         val result = transformer.process(".foo { display: flex; }")
 *         // assertions...
 *     }
 * }
 * ```
 */
class PostCssExtension :
    BeforeAllCallback,
    ParameterResolver {
    companion object {
        private val NAMESPACE = ExtensionContext.Namespace.create("kotlin-htmx", "postcss")
        private const val TRANSFORMER_KEY = "postCssTransformer"

        /**
         * Get or create a shared PostCssTransformer from the extension context.
         * Can be called from other extensions to share the same transformer instance.
         */
        fun getOrCreateTransformer(context: ExtensionContext): PostCssTransformer {
            val rootStore = context.root.getStore(NAMESPACE)
            val existing = rootStore.get(TRANSFORMER_KEY, PostCssTransformer::class.java)
            if (existing != null) {
                return existing
            }
            val transformer = PostCssTransformer(poolSize = 2)
            rootStore.put(TRANSFORMER_KEY, transformer)
            return transformer
        }
    }

    override fun beforeAll(context: ExtensionContext) {
        // Use the shared helper to ensure transformer is created
        getOrCreateTransformer(context)
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean = parameterContext.parameter.type == PostCssTransformer::class.java

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any {
        val rootStore = extensionContext.root.getStore(NAMESPACE)
        return rootStore.get(TRANSFORMER_KEY, PostCssTransformer::class.java)
            ?: throw IllegalStateException("PostCssTransformer not found in ExtensionContext.Store")
    }
}
