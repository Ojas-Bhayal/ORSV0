package com.sunilos.p4.ctl;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import com.sunilos.p4.util.MessageSource;

/**
 * Persists the selected UI language for this user's session and sets the
 * request-thread locale before any JSP or custom message tag is rendered.
 * The actual locale is thread-local in MessageSource so simultaneous users
 * cannot change each other's language.
 */
@WebFilter("/*")
public class LanguageFilter implements Filter {
    private static final String SESSION_LANGUAGE = "orsLanguage";
    private static final String REQUEST_LANGUAGE = "orsLanguage";

    @Override
    public void init(FilterConfig config) throws ServletException {
        // No initialization required.
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpSession session = request.getSession(false);

        String rawLanguage = request.getParameter("lang");
        String requested = rawLanguage == null ? "" : rawLanguage.trim().toLowerCase(java.util.Locale.ROOT);
        if (isSupported(requested)) {
            if (session == null) {
                session = request.getSession(true);
            }
            session.setAttribute(SESSION_LANGUAGE, requested);
        }

        String language = MessageSource.DEFAULT_LANGUAGE;
        if (session != null) {
            Object saved = session.getAttribute(SESSION_LANGUAGE);
            if (saved != null && isSupported(String.valueOf(saved))) {
                language = String.valueOf(saved).toLowerCase();
            }
        }
        if (isSupported(requested)) {
            language = requested;
        }

        MessageSource messages = MessageSource.getInstance();
        messages.setLocale(language);
        request.setAttribute(REQUEST_LANGUAGE, language);
        try {
            chain.doFilter(servletRequest, servletResponse);
        } finally {
            messages.clearLocale();
        }
    }

    private boolean isSupported(String language) {
        return MessageSource.ENGLISH_LANG_CODE.equals(language) || MessageSource.HINDI_LANG_CODE.equals(language);
    }

    @Override
    public void destroy() {
        // No resources to release.
    }
}
