import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlSpan
import com.gargoylesoftware.htmlunit.html.HtmlTextArea
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kink.Kink
import kink.KinkRepository
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object BdsmScrapeUtil {
    private val SCORE_REGEX = Regex("""(-?\d+)% ([a-zA-Z /\-()]+) """)
    private val webClient = buildWebClient()

    /**
     * Scrapes the website for the results of a test.
     * @return the text version of the results with the given result id
     */
    fun getParsedResults(id: String, jsWaitingDuration: Long = 2000L): Result {
        webClient.use {
            val url = "https://bdsmtest.org/r/$id"
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
     * Scrapes the website for the all result ids associated with an account.
     * @returns a list of ids of all results of an account
     */
    fun getResultIdsForUser(email: String, password: String): List<String> = runBlocking {
        val client = HttpClient(CIO)
        val loginResponse: HttpResponse = client.submitForm(
            url = "https://bdsmtest.org/ajax/login",
            formParameters = Parameters.build {
                append("email", email)
                append("pass", password)
            }
        )
        assert(loginResponse.status == HttpStatusCode.OK)
        val userAuth = parseUserAuth(loginResponse.bodyAsText())
        val profileResponse: HttpResponse = client.submitForm(
            url = "https://bdsmtest.org/ajax/myresults",
            formParameters = Parameters.build {
                append("uauth[uid]", userAuth.uid)
                append("uauth[salt]", userAuth.salt)
                append("uauth[authsig]", userAuth.authsig)
            }
        )
        assert(profileResponse.status == HttpStatusCode.OK)
        client.close()
        return@runBlocking parseTestResultIds(profileResponse.bodyAsText())
    }

    private fun parseUserAuth(json: String): UserAuth {
        val jsonErrorText = "malformed JSON (uauth)"
        val parser: Parser = Parser.default()
        val loginJson = parser.parse(StringBuilder(json)) as JsonObject
        val userAuthJson = loginJson.obj("uauth") ?: throw IllegalArgumentException(jsonErrorText)
        return UserAuth(
            userAuthJson.string("uid") ?: throw IllegalArgumentException(jsonErrorText),
            userAuthJson.string("salt") ?: throw IllegalArgumentException(jsonErrorText),
            userAuthJson.string("authsig") ?: throw IllegalArgumentException(jsonErrorText),
        )
    }

    private fun parseTestResultIds(json: String): List<String> {
        val jsonErrorText = "malformed JSON (rids)"
        val parser: Parser = Parser.default()
        val resultJson = parser.parse(StringBuilder(json)) as JsonObject
        val resultIdsJson = resultJson.array<JsonObject>("rids") ?: throw IllegalArgumentException(jsonErrorText)
        return resultIdsJson.map {
            it.obj("rauth")?.string("rid") ?: throw IllegalArgumentException(jsonErrorText)
        }
    }

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
    println(BdsmScrapeUtil.getParsedResults(id = "3382364"))
    println(BdsmScrapeUtil.getResultIdsForUser("<email>", "<password>"))
}