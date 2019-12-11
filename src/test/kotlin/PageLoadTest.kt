import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.remote.DesiredCapabilities
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import org.openqa.selenium.logging.LogType
import org.openqa.selenium.logging.LoggingPreferences
import java.util.logging.Level


class PageLoadTest {

    @Test
    fun `page loaded`() {
        val options = ChromeOptions()
        options.addArguments("--incognito"/*, "headless"*/)
        val capabilities = DesiredCapabilities.chrome()
        capabilities.setCapability(ChromeOptions.CAPABILITY, options)

        enableNetworkLogging(capabilities)

        val pageUrl = "https://kiba-d.github.io/selenium-test.html"

        val start = System.currentTimeMillis()

        val windowsCount = 10
        for (x in 1..windowsCount) {
            val driver = ChromeDriver(capabilities)
            driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS)
            driver.run {
                get(pageUrl)
                sleep(1000)
                try {
                    //can be replaced with browsermob proxt https://octopus.com/blog/selenium/11-adding-the-browsermob-proxy/adding-the-browsermob-proxy
                    val logs = performanceLogs(driver)
                    val successfulResponses = logs.all.filter {
                        it.message.contains("Network.responseReceived") && it.message.contains("googleadservices")
                                &&
                                it.message.contains("selenium_page_load")  &&  it.message.contains(""""status":200""")
                    }
                    assertTrue(successfulResponses.any())
                    assertTrue(driver.manage().getCookieNamed("_gid").value.any())
                }
                catch (ex: Throwable) {
                    println("Exception occurred on $x iteration")
                    throw ex
                }
                finally {
                    quit()
                }
                if (x % 10 == 0) {
                    println("Progress: $x/$windowsCount")
                }
            }
        }

        val executionTime = System.currentTimeMillis() - start
        println("It took $executionTime ms to open $windowsCount windows")
    }

    private fun performanceLogs(driver: ChromeDriver) = driver.manage().logs().get("performance")

    private fun enableNetworkLogging(capabilities: DesiredCapabilities) {
        val logPrefs = LoggingPreferences()
        logPrefs.enable(LogType.PERFORMANCE, Level.ALL)
        capabilities.setCapability("goog:loggingPrefs", logPrefs)
    }
}
