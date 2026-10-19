package com.sunilos.p4.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Thread-safe message bundle facade. Locale is held per request thread rather
 * than in a single mutable global ResourceBundle, so users in different
 * sessions can use different languages concurrently.
 */
public final class MessageSource {
    public static final String ENGLISH_LANG_CODE = "en";
    public static final String HINDI_LANG_CODE = "hi";
    public static final String DEFAULT_LANGUAGE = ENGLISH_LANG_CODE;

    private static final MessageSource INSTANCE = new MessageSource();
    private static final ThreadLocal<String> CURRENT_LANGUAGE =
            ThreadLocal.withInitial(() -> DEFAULT_LANGUAGE);

    private MessageSource() {
    }

    public static MessageSource getInstance() {
        return INSTANCE;
    }

    /** Select a locale for the current request thread. */
    public static MessageSource getInstance(String languageCode) {
        INSTANCE.setLocale(languageCode);
        return INSTANCE;
    }

    public void setLocale(String languageCode) {
        String normalized = languageCode == null ? DEFAULT_LANGUAGE : languageCode.trim().toLowerCase(Locale.ROOT);
        if (!ENGLISH_LANG_CODE.equals(normalized) && !HINDI_LANG_CODE.equals(normalized)) {
            normalized = DEFAULT_LANGUAGE;
        }
        CURRENT_LANGUAGE.set(normalized);
    }

    /** Clear thread-local state when a request finishes, preventing thread reuse leaks. */
    public void clearLocale() {
        CURRENT_LANGUAGE.remove();
    }

    public String getLanguage() {
        String language = CURRENT_LANGUAGE.get();
        return language == null ? DEFAULT_LANGUAGE : language;
    }

    public String get(String key) {
        if (key == null || key.trim().isEmpty()) {
            return "";
        }
        String language = getLanguage();
        try {
            ResourceBundle selected = ResourceBundle.getBundle("message", new Locale(language));
            if (selected.containsKey(key)) {
                return selected.getString(key);
            }
        } catch (MissingResourceException ignored) {
            // Fall through to the English bundle.
        }
        try {
            ResourceBundle english = ResourceBundle.getBundle("message", Locale.ENGLISH);
            return english.containsKey(key) ? english.getString(key) : key;
        } catch (MissingResourceException ignored) {
            return key;
        }
    }
}
