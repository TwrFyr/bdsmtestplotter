import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.HtmlPage
import com.gargoylesoftware.htmlunit.html.HtmlTextArea

/**
 * @return the URL for the results with the given id
 */
fun getResultUrl(id: Int) = "https://bdsmtest.org/r/$id"

/**
 * Scrapes the website for the results of a test.
 * @return the text version of the results with the given id
 */
fun getParsedResults(id: Int, jsWaitingDuration: Long = 2000L): String {
    val webClient = WebClient(BrowserVersion.CHROME)
    webClient.options.isThrowExceptionOnScriptError = false
    webClient.ajaxController = NicelyResynchronizingAjaxController()
    webClient.use {
        val url = getResultUrl(id)
        val page = webClient.getPage<HtmlPage>(url)
        webClient.waitForBackgroundJavaScript(jsWaitingDuration)
        val resultTextArea = page.getHtmlElementById<HtmlTextArea>("copypastearea")
        return resultTextArea.text
    }
}

fun main() {
    print(getParsedResults(3382364))
}