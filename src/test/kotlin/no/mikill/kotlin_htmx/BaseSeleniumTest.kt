package no.mikill.kotlin_htmx

import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.openqa.selenium.Dimension
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Point
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import java.time.Duration

abstract class BaseSeleniumTest {
    protected lateinit var server: EmbeddedServer<*, *>
    protected var serverUrl: String? = null
    protected open val headless: Boolean = true

    @BeforeEach
    open fun setUp() {
        startServer()
    }

    @AfterEach
    open fun tearDown() {
        stopServer()
    }

    private fun startServer() {
        server =
            embeddedServer(Netty, port = 0, host = "0.0.0.0") {
                module()
            }.start(wait = false)

        val port =
            runBlocking {
                server.engine
                    .resolvedConnectors()
                    .first()
                    .port
            }
        serverUrl = "http://localhost:$port"
    }

    private fun stopServer() {
        server.stop(1000, 2000)
    }

    protected fun createWebDriver(userDataSuffix: String = ""): WebDriver {
        val baseTimestamp = System.currentTimeMillis()
        val randomSuffix = kotlin.random.Random.nextInt(10000, 99999)

        val options = ChromeOptions()
        if (headless) options.addArguments("--headless")
        options.addArguments("--disable-gpu")
        options.addArguments("--no-sandbox")
        options.addArguments("--disable-dev-shm-usage")
        options.addArguments("--disable-extensions")
        options.addArguments("--disable-web-security")
        options.addArguments("--user-data-dir=/tmp/chrome-user-data-$baseTimestamp-${randomSuffix}$userDataSuffix")

        val driver = ChromeDriver(options)
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10))

        return driver
    }

    protected fun getScreenDimensions(driver: WebDriver): Pair<Int, Int> {
        driver.manage().window().maximize()
        val js = driver as JavascriptExecutor
        val screenWidth = js.executeScript("return window.screen.width") as Long
        val screenHeight = js.executeScript("return window.screen.height") as Long
        return Pair(screenWidth.toInt(), screenHeight.toInt())
    }

    protected fun configureDualDriverWindows(
        driver1: WebDriver,
        driver2: WebDriver,
    ) {
        if (!headless) {
            val (screenWidth, screenHeight) = getScreenDimensions(driver1)
            driver1.manage().window().size = Dimension(screenWidth / 2, screenHeight)
            driver2.manage().window().size = Dimension(screenWidth / 2, screenHeight)
            driver1.manage().window().position = Point(0, 0)
            driver2.manage().window().position = Point(screenWidth / 2, 0)
        }
    }
}
