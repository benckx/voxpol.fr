package fr.voxpol.webapp.rendering

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*

suspend fun ApplicationCall.renderSiteMap() {
    val baseUrl = "https://voxpol.fr"
    val urls = listOf(
        "/",
        "/premier-tour-2027",
        "/second-tour-2027",
    )
    
    val sitemap = buildString {
        appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
        appendLine("""<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""")
        for (path in urls) {
            appendLine("  <url>")
            appendLine("    <loc>$baseUrl$path</loc>")
            appendLine("    <changefreq>weekly</changefreq>")
            appendLine("  </url>")
        }
        append("</urlset>")
    }
    respondText(sitemap, ContentType.Text.Xml, HttpStatusCode.OK)
}
