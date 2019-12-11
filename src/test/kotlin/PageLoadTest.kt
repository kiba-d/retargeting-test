import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.DesiredCapabilities
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import org.slf4j.LoggerFactory
import java.util.logging.Level


class PageLoadTest {

    private val logger = LoggerFactory.getLogger(PageLoadTest::class.java)

    @Test
    fun `page loaded`() {
        val options = ChromeOptions()
        options.addArguments("--incognito"/*, "headless"*/)
        val capabilities = DesiredCapabilities.chrome()
        capabilities.setCapability(ChromeOptions.CAPABILITY, options)

        enableNetworkLogging(capabilities)

        val pageUrl = "https://kiba-d.github.io/selenium-test.html"

        val start = System.currentTimeMillis()

        val windowsCount = 20
        var failed = 0
        var totalRetries = 0
        for (x in 1..windowsCount) {
            val driver = ChromeDriver(capabilities)
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)
            driver.get(pageUrl)

            sleep(500)

            var tracked = tracked(driver)
            var retries  = 5
            while(!tracked && retries > 0){
                tracked = tracked(driver)
                if (!tracked) {
                    retries--
                    totalRetries++
                }
                sleep(500)
            }
            if (retries == 0) {
                failed++
            }

            driver.quit()

            if (x % 50 == 0) {
                println("Progress: $x/$windowsCount")
            }

        }

        val executionTime = System.currentTimeMillis() - start
        println("It took $executionTime ms to open $windowsCount windows")
        println("Failed: $failed")
        println("Total retries: $totalRetries")
    }

    private fun tracked(driver: ChromeDriver): Boolean {
        val logs = performanceLogs(driver)

        //can be replaced with browsermob proxt https://octopus.com/blog/selenium/11-adding-the-browsermob-proxy/adding-the-browsermob-proxy
        val successfulResponses = logs.all.filter {
            it.message.contains("Network.responseReceived") && it.message.contains("googleadservices")
                    &&
                    it.message.contains("selenium_page_load") && it.message.contains(""""status":200""")
        }
        return successfulResponses.any() && driver.manage().getCookieNamed("_gid").value.any()
    }

    private fun performanceLogs(driver: ChromeDriver) = driver.manage().logs().get("performance")

    private fun enableNetworkLogging(capabilities: DesiredCapabilities) {
        val logPrefs = LoggingPreferences()
        logPrefs.enable(LogType.PERFORMANCE, Level.ALL)
        capabilities.setCapability("goog:loggingPrefs", logPrefs)
    }
}
