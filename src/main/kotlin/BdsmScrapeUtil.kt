import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlSpan
import com.gargoylesoftware.htmlunit.html.HtmlTextArea
import kink.Kink
import kink.KinkRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object BdsmScrapeUtil {
    private val SCORE_REGEX = Regex("""(-?\d+)% ([a-zA-Z /\-()]+) """)
    private val webClient = buildWebClient()

    /**
     * Scrapes the website for the results of a test.
     * @return the text version of the results with the given id
     */
    fun getParsedResults(id: Int, jsWaitingDuration: Long = 2000L): Result {
        webClient.use {
            val url = getResultUrl(id)
            val page = webClient.getPage<HtmlPage>(url)
            webClient.waitForBackgroundJavaScript(jsWaitingDuration)

            val resultTextArea = page.getHtmlElementById<HtmlTextArea>("copypastearea")
            val resultDate = page.getHtmlElementById<HtmlSpan>("resultdate")

            return Result(
                id = id,
                date = LocalDate.parse(resultDate.asNormalizedText(), DateTimeFormatter.ISO_DATE),
                kinkMap = parseResultText(resultTextArea.text)
            )
        }
    }

    /**
     * @return the URL for the results with the given id
     */
    private fun getResultUrl(id: Int) = "https://bdsmtest.org/r/$id"

    private fun buildWebClient(): WebClient {
        val webClient = WebClient(BrowserVersion.CHROME)
        webClient.options.isThrowExceptionOnScriptError = false
        webClient.ajaxController = NicelyResynchronizingAjaxController()
        return webClient
    }

    private fun parseResultText(text: String): Map<Kink, Int> {
        val map = mutableMapOf<Kink, Int>()
        val matches = SCORE_REGEX.findAll(text)
        matches.forEach { match ->
            val percentage = match.groups[1]?.value?.toInt() ?: 0
            val name = match.groups[2]?.value ?: ""
            map[KinkRepository.getKinkByName(name)] = percentage
        }
        return map
    }
}

fun main() {
    print(BdsmScrapeUtil.getParsedResults(id = 3382364))
}