package com.example.meta;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.lang.reflect.Method;

public class PageMetaExtractorTest {

    @Test
    void testMetaExtractionFromHtml() throws Exception {
        // Create a simple fake HTML page
        String html = """
            <html>
                <head>
                    <meta property="og:title" content="Sample Page Title">
                    <meta name="author" content="John Doe">
                </head>
                <body>
                    <h1>Sample Page Title</h1>
                    <p>Some text content here.</p>
                </body>
            </html>
        """;

        // Parse it using Jsoup (same as in your main code)
        Document doc = Jsoup.parse(html);

        // Use reflection to call private methods for testing
        Method metaProp = PageMetaExtractor.class.getDeclaredMethod("metaProp", Document.class, String.class);
        metaProp.setAccessible(true);
        String title = (String) metaProp.invoke(null, doc, "og:title");

        Method metaName = PageMetaExtractor.class.getDeclaredMethod("metaName", Document.class, String.class);
        metaName.setAccessible(true);
        String author = (String) metaName.invoke(null, doc, "author");

        // Verify extracted metadata
        assertEquals("Sample Page Title", title);
        assertEquals("John Doe", author);
    }

    @Test
    void testIsBlankUtility() throws Exception {
        Method isBlank = PageMetaExtractor.class.getDeclaredMethod("isBlank", String.class);
        isBlank.setAccessible(true);

        assertTrue((Boolean) isBlank.invoke(null, ""));
        assertTrue((Boolean) isBlank.invoke(null, "   "));
        assertFalse((Boolean) isBlank.invoke(null, "Not Blank"));
    }
}