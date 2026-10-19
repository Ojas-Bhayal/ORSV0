package com.sunilos.p4.ctl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Regression tests for fail-closed CSRF validation. */
class BaseCtlCsrfTest {

    private HttpSession sessionWithToken(String token) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            if ("getAttribute".equals(method.getName()) && args != null && args.length == 1
                    && "csrfToken".equals(args[0])) {
                return token;
            }
            return defaultValue(method.getReturnType());
        };
        return (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                new Class<?>[] { HttpSession.class }, handler);
    }

    private HttpServletRequest request(HttpSession session, String parameterToken, String headerToken) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            if ("getSession".equals(method.getName())) {
                return session;
            }
            if ("getParameter".equals(method.getName()) && args != null && args.length == 1
                    && "csrfToken".equals(args[0])) {
                return parameterToken;
            }
            if ("getHeader".equals(method.getName()) && args != null && args.length == 1
                    && "X-CSRF-Token".equals(args[0])) {
                return headerToken;
            }
            return defaultValue(method.getReturnType());
        };
        return (HttpServletRequest) Proxy.newProxyInstance(HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class }, handler);
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }

    @Test
    void rejectsNullRequestAndMissingSession() {
        assertFalse(BaseCtl.verifyCsrfToken(null));
        assertFalse(BaseCtl.verifyCsrfToken(request(null, "token", null)));
    }

    @Test
    void rejectsMissingExpectedOrSubmittedTokens() {
        assertFalse(BaseCtl.verifyCsrfToken(request(sessionWithToken(null), "token", null)));
        assertFalse(BaseCtl.verifyCsrfToken(request(sessionWithToken("expected"), null, null)));
        assertFalse(BaseCtl.verifyCsrfToken(request(sessionWithToken("expected"), "   ", "   ")));
    }

    @Test
    void rejectsMismatchedToken() {
        assertFalse(BaseCtl.verifyCsrfToken(request(sessionWithToken("expected"), "attacker", null)));
    }

    @Test
    void acceptsMatchingFormOrHeaderToken() {
        assertTrue(BaseCtl.verifyCsrfToken(request(sessionWithToken("expected"), "expected", null)));
        assertTrue(BaseCtl.verifyCsrfToken(request(sessionWithToken("expected"), null, "expected")));
    }


    @Test
    void ensureCsrfTokenPreservesExistingSessionToken() {
        AtomicReference<Object> stored = new AtomicReference<>("stable-token");
        HttpSession session = (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                new Class<?>[] { HttpSession.class }, (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()) && args != null
                            && args.length == 1 && "csrfToken".equals(args[0])) {
                        return stored.get();
                    }
                    if ("setAttribute".equals(method.getName()) && args != null
                            && args.length == 2 && "csrfToken".equals(args[0])) {
                        stored.set(args[1]);
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });

        BaseCtl.ensureCsrfToken(session);
        assertEquals("stable-token", stored.get(), "Existing CSRF token must not rotate on page navigation");
    }

    @Test
    void ensureCsrfTokenCreatesTokenWhenMissing() {
        AtomicReference<Object> stored = new AtomicReference<>(null);
        HttpSession session = (HttpSession) Proxy.newProxyInstance(HttpSession.class.getClassLoader(),
                new Class<?>[] { HttpSession.class }, (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()) && args != null
                            && args.length == 1 && "csrfToken".equals(args[0])) {
                        return stored.get();
                    }
                    if ("setAttribute".equals(method.getName()) && args != null
                            && args.length == 2 && "csrfToken".equals(args[0])) {
                        stored.set(args[1]);
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });

        BaseCtl.ensureCsrfToken(session);
        assertNotNull(stored.get());
        assertEquals(64, String.valueOf(stored.get()).length());
    }

}
