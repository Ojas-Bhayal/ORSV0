package com.sunilos.p4.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class HTMLUtilityTest {
    @Test
    void escapesHtmlTextAndAttributeCharacters() {
        assertEquals("&lt;script&gt;alert(&quot;x&quot;)&lt;/script&gt;", HTMLUtility.escapeHtml("<script>alert(\"x\")</script>"));
        assertEquals("O&#39;Reilly &amp; Co", HTMLUtility.escapeHtml("O'Reilly & Co"));
    }

    @Test
    void nullEscapesToEmptyString() {
        assertEquals("", HTMLUtility.escapeHtml(null));
    }
}
