package no.mikill.kotlin_htmx

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import kotlin.random.Random

class HtmxCheckboxPageTest : BaseSeleniumTest() {
    override val headless = true
    private val checkboxPageUrl = "/demo/htmx/checkboxes"
    private lateinit var driver1: WebDriver
    private lateinit var driver2: WebDriver

    @BeforeEach
    override fun setUp() {
        super.setUp()

        driver1 = createWebDriver("-1")
        driver2 = createWebDriver("-2")

        configureDualDriverWindows(driver1, driver2)
    }

    @AfterEach
    override fun tearDown() {
        driver1.quit()
        driver2.quit()

        super.tearDown()
    }

    @Test
    fun testHtmxCheckboxPage() {
        // Navigate to the checkbox page
        fun WebDriver.openAndScrollToCheckbox() {
            get(serverUrl!! + checkboxPageUrl)
            if (!headless) {
                findElement(By.id("1"))
                    .also { (this as JavascriptExecutor).executeScript("arguments[0].scrollIntoView(true)", it) }
            }

            // Wait for the page to load
            val wait1 = WebDriverWait(this, Duration.ofSeconds(10))
            wait1.until(ExpectedConditions.presenceOfElementLocated(By.tagName("input")))

            // Verify the page title contains expected text
            val pageTitle = this.title
            assertThat(pageTitle).contains("HTMX + SSE Checkboxes demo")

            // Verify page content contains expected text
            val pageContent = this.findElement(By.tagName("body")).text
            assertThat(pageContent).contains("This page shows synchronization between browser windows")
        }

        driver1.openAndScrollToCheckbox()
        driver2.openAndScrollToCheckbox()

        // Find subset of boxes to test with
        // TODO: This probably doesn't work if we use infinite scroll batches are smaller than 100
        val numberOfCheckboxes = 100
        val checkboxes =
            driver1
                .findElements(By.tagName("input"))
                .take(numberOfCheckboxes)
                .associate { checkbox ->
                    checkbox.getDomAttribute("id")!! to
                        checkbox.isSelected
                }.toMutableMap()

        val randomCheckboxIds =
            generateSequence { Random.nextInt(0, numberOfCheckboxes) }
                .distinct()
                .take(20)
                .map { it.toString() }
                .toList()

        // Click only the checkboxes in randomCheckboxIds
        randomCheckboxIds.forEach { checkboxId ->
            // Re-find the element before each click to avoid stale element references
            val checkbox1 = driver1.findElement(By.id(checkboxId))
            checkbox1.click()
            checkboxes[checkboxId] = checkboxes[checkboxId]!!.not() // Flip state
            // Small wait between clicks to allow the page to update
            Thread.sleep(if (!headless) 300 else 10)
        }

        // Wait once for all updates to fully propagate to the second browser
        Thread.sleep(1000)

        // Verify that checkboxes has correct state across both browsers
        randomCheckboxIds.forEach { checkboxId ->
            // Re-find the checkbox in both browsers (to avoid stale element reference)
            val updatedCheckbox1 = driver1.findElement(By.id(checkboxId))
            val updatedCheckbox2 = driver2.findElement(By.id(checkboxId))

            val expectedState = checkboxes[checkboxId]!!

            // Verify the checkbox state has changed in the first browser
            val newState1 = updatedCheckbox1.isSelected
            assertThat(newState1).isEqualTo(expectedState)

            // Verify the checkbox state has also changed in the second browser
            val newState2 = updatedCheckbox2.isSelected
            assertThat(newState2).isEqualTo(expectedState)
        }
    }
}
