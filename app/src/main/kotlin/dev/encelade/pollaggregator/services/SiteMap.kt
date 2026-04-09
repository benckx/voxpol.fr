package dev.encelade.pollaggregator.services

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.siteMap() {
    get("/sitemap.xml") {
        val baseUrl = "https://voxpol.fr"
        val urls = listOf(
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
        call.respondText(sitemap, ContentType.Text.Xml, HttpStatusCode.OK)
    }
}
