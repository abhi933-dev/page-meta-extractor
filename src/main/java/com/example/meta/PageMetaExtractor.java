package com.example.meta;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PageMetaExtractor {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java -jar page-meta-extractor-1.0.0.jar <url>");
            System.exit(1);
        }
        String url = args[0];
        Document doc;
        try {
            doc = Jsoup
                    .connect(url)
                    .userAgent("MetaExtractor/1.0 (+https://example.com)")
                    .timeout((int) Duration.ofSeconds(15).toMillis())
                    .followRedirects(true)
                    .get();
        } catch (SocketTimeoutException e) {
            emitJson(url, null, null, "timeout");
            return;
        } catch (Exception e) {
            emitJson(url, null, null, e.getClass().getSimpleName().toLowerCase());
            return;
        }
        String resolved = doc.location();
        String title = firstNonBlank(
                metaProp(doc, "og:title"),
                metaName(doc, "twitter:title"),
                textOrNull(doc.selectFirst("title")),
                textOrNull(doc.selectFirst("h1")),
                metaName(doc, "title")
        );
        String author = firstNonBlank(
                metaName(doc, "author"),
                metaProp(doc, "article:author"),
                metaName(doc, "byline"),
                authorFromBylineHeuristic(doc)
        );
        emitJson(url, resolved, safe(title), safe(author), null);
    }

    private static String metaProp(Document doc, String property) {
        Element e = doc.selectFirst("meta[property=" + property + "]");
        return contentOrNull(e);
    }

    private static String metaName(Document doc, String name) {
        Element e = doc.selectFirst("meta[name=" + name + "]");
        return contentOrNull(e);
    }

    private static String contentOrNull(Element e) {
        if (e == null) return null;
        String v = e.attr("content");
        return v == null || v.trim().isEmpty() ? null : v.trim();
    }

    private static String textOrNull(Element e) {
        if (e == null) return null;
        String v = e.text();
        return v == null || v.trim().isEmpty() ? null : v.trim();
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (!isBlank(v)) return v.trim();
        }
        return null;
    }

    private static String safe(String s) {
        return isBlank(s) ? null : s;
    }

    private static final Pattern BYLINE = Pattern.compile("\b[Bb]y\s+([A-Z][A-Za-z.'\-]+(?:\s+[A-Z][A-Za-z.'\-]+){0,3})\b");

    private static String authorFromBylineHeuristic(Document doc) {
        List<String> selectorCandidates = List.of(
                "[class*=byline]",
                "[class*=author]",
                "header, .article-header, .post-header, .entry-header",
                "meta[itemprop=author]"
        );
        for (String sel : selectorCandidates) {
            Element e = doc.selectFirst(sel);
            if (e != null) {
                String t = e.text();
                if (!isBlank(t)) {
                    Matcher m = BYLINE.matcher(t);
                    if (m.find()) return m.group(1);
                }
            }
        }
        String body = doc.text();
        body = body.length() > 1000 ? body.substring(0, 1000) : body;
        Matcher m = BYLINE.matcher(body);
        return m.find() ? m.group(1) : null;
    }

    private static void emitJson(String input, String resolved, String title, String author) throws Exception {
        emitJson(input, resolved, title, author, null);
    }

    private static void emitJson(String input, String resolved, String title, String author, String error) throws Exception {
        ObjectMapper om = new ObjectMapper();
        ObjectNode root = om.createObjectNode();
        root.put("inputUrl", input);
        if (resolved != null) root.put("resolvedUrl", resolved);
        if (title != null) root.put("title", title);
        if (author != null) root.put("author", author);
        if (error != null) root.put("error", error);
        System.out.println(om.writerWithDefaultPrettyPrinter().writeValueAsString(root));
    }
}
