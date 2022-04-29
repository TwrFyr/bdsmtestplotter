package scraping

import model.Result

interface BdsmScraper {
    /**
     * Scrapes the website for the results of a test.
     * @return the parsed result with the given id
     */
    fun getParsedResults(resultId: String): Result

    /**
     * Scrapes the website for the all result ids associated with an account.
     * @returns a list of ids of all results of an account
     */
    fun getResultIdsForUser(email: String, password: String): List<String>
}
