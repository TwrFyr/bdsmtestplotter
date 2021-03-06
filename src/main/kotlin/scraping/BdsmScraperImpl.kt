package scraping

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
import kotlinx.coroutines.runBlocking
import model.Kink
import model.TestResult
import persistence.KinkRepository
import persistence.ResultRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object BdsmScraperImpl : BdsmScraper {
    private const val AUTH_SIGNATURE = "814a69afc15258000678f00526b0c107ac271b5ea997beb4f7c1e81c861c972b"
    private val SCORE_REGEX = Regex("""(-?\d+)% ([a-zA-Z /\-()]+) """)
    private val webClient = buildWebClient()
    private val parser: Parser = Parser.default()

    /**
     * Scrapes the website for the results of a test.
     * @return the text version of the results with the given result id
     */
    fun getParsedResults(resultId: String, jsWaitingDuration: Long = 2000L): TestResult {
        webClient.use {
            val url = "https://bdsmtest.org/r/$resultId"
            val page = webClient.getPage<HtmlPage>(url)
            webClient.waitForBackgroundJavaScript(jsWaitingDuration)

            val resultTextArea = page.getHtmlElementById<HtmlTextArea>("copypastearea")
            val resultDate = page.getHtmlElementById<HtmlSpan>("resultdate")

            return TestResult(
                resultId = resultId,
                date = LocalDate.parse(resultDate.asNormalizedText(), DateTimeFormatter.ISO_DATE),
                kinkMap = parseResultText(resultTextArea.text)
            )
        }
    }

    override fun getParsedResults(resultId: String): TestResult = runBlocking {
        val client = HttpClient(CIO)
        val resultResponse: HttpResponse = client.submitForm(
            url = "https://bdsmtest.org/ajax/getresult",
            formParameters = Parameters.build {
                append("uauth[uid]", "0")
                append("uauth[salt]", "")
                append("uauth[authsig]", AUTH_SIGNATURE)
                append("rauth[rid]", resultId)
            }
        )
        assert(resultResponse.status == HttpStatusCode.OK)
        client.close()

        return@runBlocking TestResult(
            resultId = resultId,
            date = parseDate(resultResponse.bodyAsText()),
            kinkMap = parseResults(resultResponse.bodyAsText()),
        )
    }

    /**
     * Parses the kink scores from the result json.
     */
    private fun parseResults(json: String): Map<Kink, Int> {
        val jsonErrorText = "malformed JSON (result score)"
        val resultsJson = parser.parse(StringBuilder(json)) as JsonObject
        val scores = resultsJson.array<JsonObject>("scores") ?: throw IllegalArgumentException(jsonErrorText)

        val resultMap = mutableMapOf<Kink, Int>()
        scores.forEach {
            val name = it.string("name") ?: throw IllegalArgumentException(jsonErrorText)
            val score = it.int("score") ?: throw IllegalArgumentException(jsonErrorText)
            resultMap[KinkRepository.getKinkByName(name)] = score
        }
        return resultMap
    }

    /**
     * Parses the date of the test from the result json.
     */
    private fun parseDate(json: String): LocalDate {
        val jsonErrorText = "malformed JSON (result date)"
        val resultsJson = parser.parse(StringBuilder(json)) as JsonObject
        val dateString = resultsJson.string("date") ?: throw IllegalArgumentException(jsonErrorText)
        return LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE)
    }

    override fun getResultIdsForUser(email: String, password: String): List<String> = runBlocking {
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
    val bdsmScraper: BdsmScraper = BdsmScraperImpl
    val resultIds = bdsmScraper.getResultIdsForUser("<email>", "<password>")
    ResultRepository.addAll(
        resultIds.map {
            bdsmScraper.getParsedResults(it)
        })
    val results = ResultRepository.getAllResults()
    print(results)
}