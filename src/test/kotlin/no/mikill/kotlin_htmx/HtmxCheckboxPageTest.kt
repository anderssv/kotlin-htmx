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

    @Test
    fun testClientCounterRendered() {
        driver1.get(serverUrl!! + checkboxPageUrl)

        // Wait for the page to load
        val wait = WebDriverWait(driver1, Duration.ofSeconds(10))
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("client-counter")))

        // Verify the client counter element exists
        val clientCounter = driver1.findElement(By.id("client-counter"))
        assertThat(clientCounter).isNotNull

        // Verify it contains the expected text
        val counterText = clientCounter.text
        assertThat(counterText).contains("Connected browsers:")
    }

    @Test
    fun testClientCounterUpdates() {
        val wait1 = WebDriverWait(driver1, Duration.ofSeconds(10))
        val wait2 = WebDriverWait(driver2, Duration.ofSeconds(10))

        // Open first browser
        driver1.get(serverUrl!! + checkboxPageUrl)
        wait1.until(ExpectedConditions.presenceOfElementLocated(By.id("client-counter")))

        // Wait a moment for SSE connection and count update
        Thread.sleep(500)

        // Verify first browser shows "Connected browsers: 1"
        val counter1 = driver1.findElement(By.id("client-counter"))
        assertThat(counter1.text).contains("Connected browsers: 1")

        // Open second browser
        driver2.get(serverUrl!! + checkboxPageUrl)
        wait2.until(ExpectedConditions.presenceOfElementLocated(By.id("client-counter")))

        // Wait for SSE connection and count updates to propagate
        Thread.sleep(500)

        // Verify both browsers show "Connected browsers: 2"
        val counter1After2nd = driver1.findElement(By.id("client-counter"))
        val counter2 = driver2.findElement(By.id("client-counter"))
        assertThat(counter1After2nd.text).contains("Connected browsers: 2")
        assertThat(counter2.text).contains("Connected browsers: 2")

        // Close second browser
        driver2.quit()

        // Wait for TCP connection to fully close before triggering dead connection detection
        Thread.sleep(5000)
        val checkbox = driver1.findElement(By.id("1"))
        checkbox.click()

        // Wait for the counter to update to "Connected browsers: 1"
        // The checkbox broadcast will detect the dead connection, remove it, and broadcast the new count
        val wait = WebDriverWait(driver1, Duration.ofSeconds(5))
        wait.until(
            ExpectedConditions.textToBePresentInElementLocated(
                By.id("client-counter"),
                "Connected browsers: 1",
            ),
        )

        // Verify first browser now shows "Connected browsers: 1"
        val counter1Final = driver1.findElement(By.id("client-counter"))
        assertThat(counter1Final.text).contains("Connected browsers: 1")

        // Recreate driver2 for teardown
        driver2 = createWebDriver("-2")
    }
}
