package no.mikill.kotlin_htmx

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

class HtmxQuestionsPageTest : BaseSeleniumTest() {

    private lateinit var driver: WebDriver
    private val questionsPageUrl = "/demo/htmx/questions"

    @BeforeEach
    override fun setUp() {
        super.setUp()
        driver = createWebDriver()
    }

    @AfterEach
    override fun tearDown() {
        driver.quit()
        super.tearDown()
    }

    @Test
    fun testHtmxQuestionsPage() = runTest {
        // Navigate to the questions page
        driver.get(serverUrl!! + questionsPageUrl)

        // Wait for the page to load
        val wait = WebDriverWait(driver, Duration.ofSeconds(10))
        wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("form")))

        // Verify the page title contains expected text
        val pageTitle = driver.title
        assertThat(pageTitle).contains("HTMX Questions Page")

        // Verify page content contains expected text
        val pageContent = driver.findElement(By.tagName("body")).text
        assertThat(pageContent).contains("Submit your questions and see what others have asked")

        // Check if the "No questions" message is displayed
        val initialContent = driver.findElement(By.id("questions-list")).text
        assertThat(initialContent).contains("No questions have been asked yet")

        // Submit a question
        val firstQuestion = "This is a test question " + System.currentTimeMillis()
        val questionInput = driver.findElement(By.id("question-input"))
        questionInput.sendKeys(firstQuestion)

        // Click the submit button
        val submitButton = driver.findElement(By.id("submit-button"))
        submitButton.click()

        // Wait for the HTMX response to update the questions list
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("questions-list"), "Asked on"))

        // Print the current content of the questions list for debugging
        val currentContent = driver.findElement(By.id("questions-list")).text

        // Verify the submitted question appears in the list
        assertThat(currentContent).contains("Asked on")
        assertThat(currentContent).contains(firstQuestion)

        // Submit another question to verify multiple questions work
        val secondQuestion = "This is another test question " + System.currentTimeMillis()

        // Verify the text box is cleared after manual clearing
        assertThat(questionInput.getAttribute("value")).isEmpty()

        questionInput.sendKeys(secondQuestion)

        // Click the submit button again
        submitButton.click()

        // Wait for the HTMX response to update the questions list with the second question
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("questions-list"), secondQuestion))

        // Print the current content of the questions list for debugging
        val updatedListContent = driver.findElement(By.id("questions-list")).text

        // Verify both questions appear in the list
        assertThat(updatedListContent).contains(firstQuestion)
        assertThat(updatedListContent).contains(secondQuestion)

        // Verify the questions are sorted with newest first
        val questionElements = driver.findElements(By.cssSelector("#questions-list li"))
        assertThat(questionElements.size).isGreaterThanOrEqualTo(2)
        assertThat(questionElements[0].text).contains(secondQuestion)
    }
}
