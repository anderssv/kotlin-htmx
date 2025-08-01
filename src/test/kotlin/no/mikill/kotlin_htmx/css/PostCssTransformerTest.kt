package no.mikill.kotlin_htmx.css

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PostCssTransformerTest {

    @Test
    fun `should process CSS with PostCSS plugins`() {
        val transformer = PostCssTransformer()
        
        val inputCss = """
            ${'$'}primary-color: #007bff;
            ${'$'}border-radius: 4px;
            
            .card {
                background-color: ${'$'}primary-color;
                border-radius: calc(${'$'}border-radius * 2);
                transform: translateX(10px);
                
                .header {
                    font-weight: bold;
                    user-select: none;
                }
            }
        """.trimIndent()

        val processedCss = transformer.process(inputCss)

        assertThat(processedCss)
            .contains("#007bff")
            .contains("8px")
            .contains(".card .header")
            .contains("-webkit-user-select: none")
            .contains("-moz-user-select: none")
            .contains("user-select: none")
            .doesNotContain("${'$'}primary-color")
            .doesNotContain("${'$'}border-radius")
            .doesNotContain("calc(")
    }

    @Test
    fun `should handle empty CSS input`() {
        val transformer = PostCssTransformer()
        
        val result = transformer.process("")
        
        assertThat(result).isEmpty()
    }

    @Test
    fun `should handle CSS without PostCSS features`() {
        val transformer = PostCssTransformer()
        
        val inputCss = """
            .simple {
                color: red;
                margin: 10px;
            }
        """.trimIndent()

        val processedCss = transformer.process(inputCss)

        assertThat(processedCss)
            .contains("color: red")
            .contains("margin: 10px")
            .contains(".simple")
    }

    @Test
    fun `should process nested selectors correctly`() {
        val transformer = PostCssTransformer()
        
        val inputCss = """
            .parent {
                color: blue;
                
                .child {
                    color: red;
                    
                    &:hover {
                        color: green;
                    }
                }
            }
        """.trimIndent()

        val processedCss = transformer.process(inputCss)

        assertThat(processedCss)
            .contains(".parent")
            .contains(".parent .child")
            .contains(".parent .child:hover")
            .doesNotContain("&:hover")
    }

    @Test
    fun `should process calc expressions`() {
        val transformer = PostCssTransformer()
        
        val inputCss = """
            .container {
                width: calc(100% - 20px);
                height: calc(50px + 10px);
                margin: calc(5px * 2);
            }
        """.trimIndent()

        val processedCss = transformer.process(inputCss)

        assertThat(processedCss)
            .contains("calc(100% - 20px)")
            .contains("60px")
            .contains("10px")
    }
}
