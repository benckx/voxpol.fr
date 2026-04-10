package fr.voxpol.webapp.utils

import fr.voxpol.webapp.services.HtmlCache
import fr.voxpol.webapp.services.PollService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.html.HTML
import kotlinx.html.html
import kotlinx.html.stream.appendHTML

private val pollService by koin<PollService>()
private val htmlCache by koin<HtmlCache>()

/**
 * Drop-in replacement for `respondHtml` that caches the rendered HTML string.
 * The request path is used as the cache key automatically.
 */
suspend fun ApplicationCall.respondHtmlCached(block: HTML.() -> Unit) {
    val key = request.path()
    val html = if (pollService.isNotEmpty()) {
        htmlCache.get(key) ?: buildString {
            append("<!DOCTYPE html>\n")
            appendHTML().html(block = block)
        }.also { htmlCache.put(key, it) }
    } else {
        buildString {
            append("<!DOCTYPE html>\n")
            appendHTML().html(block = block)
        }
    }
    respondText(html, ContentType.Text.Html)
}
