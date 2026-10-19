package com.sunilos.p4.util;

import java.util.Date;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.sunilos.p4.exception.ApplicationException;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * SMTP email utility. Credentials must be supplied through environment variables
 * or JVM properties; no usable mail credentials are bundled with the application.
 */
public final class EmailUtility {
    private static final Logger log = Logger.getLogger(EmailUtility.class);
    private static final ResourceBundle RB = ResourceBundle.getBundle("com.sunilos.p4.bundle.system");

    private EmailUtility() { }

    private static String config(String envName, String propertyKey, String fallback) {
        String value = System.getenv(envName);
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(envName);
        }
        if (value != null && !value.trim().isEmpty()) {
            return value.trim();
        }
        try {
            value = RB.getString(propertyKey);
        } catch (Exception ignored) {
            return fallback;
        }
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        value = value.trim();
        if (value.startsWith("${") && value.endsWith("}")) {
            int colon = value.indexOf(':');
            return colon > 0 ? value.substring(colon + 1, value.length() - 1).trim() : fallback;
        }
        return value;
    }

    private static MailConfig currentConfig() {
        return new MailConfig(
                config("ORS_SMTP_HOST", "smtp.server", "smtp.gmail.com"),
                config("ORS_SMTP_PORT", "smtp.port", "587"),
                config("ORS_SMTP_USER", "email.login", ""),
                config("ORS_SMTP_PASSWORD", "email.pwd", ""));
    }

    private static void validateConfig(MailConfig c) {
        boolean placeholderUser = c.user == null || c.user.trim().isEmpty()
                || c.user.contains("${") || c.user.toLowerCase().endsWith(".example.com");
        boolean missingPassword = c.password == null || c.password.trim().isEmpty()
                || c.password.contains("${");
        if (placeholderUser || missingPassword) {
            throw new ApplicationException("Password-reset email is not configured. Set ORS_SMTP_USER and ORS_SMTP_PASSWORD (a Gmail App Password for Gmail), then restart Tomcat.");
        }
        try {
            int port = Integer.parseInt(c.port);
            if (port < 1 || port > 65535 || c.host == null || c.host.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid SMTP host/port");
            }
            InternetAddress address = new InternetAddress(c.user, true);
            address.validate();
        } catch (Exception e) {
            throw new ApplicationException("Invalid SMTP configuration. Check ORS_SMTP_HOST, ORS_SMTP_PORT and ORS_SMTP_USER.");
        }
    }

    private static Properties smtpProperties(MailConfig c) {
        Properties p = new Properties();
        p.setProperty("mail.smtp.host", c.host);
        p.setProperty("mail.smtp.port", c.port);
        p.setProperty("mail.smtp.auth", "true");
        p.setProperty("mail.smtp.connectiontimeout", "10000");
        p.setProperty("mail.smtp.timeout", "10000");
        p.setProperty("mail.smtp.writetimeout", "10000");
        p.setProperty("mail.debug", "false");
        if ("465".equals(c.port)) {
            p.setProperty("mail.smtp.ssl.enable", "true");
        } else {
            p.setProperty("mail.smtp.starttls.enable", "true");
            p.setProperty("mail.smtp.starttls.required", "true");
        }
        return p;
    }

    private static Session newSession(MailConfig c) {
        return Session.getInstance(smtpProperties(c));
    }

    /**
     * Checks settings and performs an authenticated SMTP connection. Call before
     * looking up an account to catch SMTP configuration errors without exposing
     * whether a submitted login exists.
     */
    public static void verifySmtpConnection() throws ApplicationException {
        MailConfig c = currentConfig();
        validateConfig(c);
        Transport transport = null;
        try {
            transport = newSession(c).getTransport("smtp");
            transport.connect(c.host, Integer.parseInt(c.port), c.user, c.password);
        } catch (Exception e) {
            log.error("SMTP preflight failed. Check the configured host, port, account, and App Password.", e);
            throw new ApplicationException("SMTP connection failed. Verify ORS_SMTP_HOST/ORS_SMTP_PORT and the ORS_SMTP_USER/ORS_SMTP_PASSWORD credentials, then restart Tomcat.");
        } finally {
            if (transport != null && transport.isConnected()) {
                try { transport.close(); } catch (MessagingException e) { log.warn("Could not close SMTP preflight connection", e); }
            }
        }
    }

    public static void sendMail(EmailMessage email) throws ApplicationException {
        if (email == null || email.getTo() == null || email.getTo().trim().isEmpty()) {
            throw new ApplicationException("Email recipient is missing.");
        }
        MailConfig c = currentConfig();
        validateConfig(c);
        Transport transport = null;
        try {
            Session session = newSession(c);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(c.user, true));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email.getTo(), true));
            if (email.getCc() != null && !email.getCc().trim().isEmpty()) {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(email.getCc(), true));
            }
            if (email.getBcc() != null && !email.getBcc().trim().isEmpty()) {
                message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(email.getBcc(), true));
            }
            message.setSubject(email.getSubject() == null ? "ORS notification" : email.getSubject(), "UTF-8");
            if (email.getMessageType() == EmailMessage.HTML_MSG) {
                message.setContent(email.getMessage() == null ? "" : email.getMessage(), "text/html; charset=UTF-8");
            } else {
                message.setText(email.getMessage() == null ? "" : email.getMessage(), "UTF-8");
            }
            message.setSentDate(new Date());
            transport = session.getTransport("smtp");
            transport.connect(c.host, Integer.parseInt(c.port), c.user, c.password);
            transport.sendMessage(message, message.getAllRecipients());
        } catch (Exception e) {
            log.error("SMTP delivery failed. Password and token values are not logged.", e);
            throw new ApplicationException("Password-reset email delivery failed. Verify SMTP configuration and review the Tomcat log for the mail-server error.");
        } finally {
            if (transport != null && transport.isConnected()) {
                try { transport.close(); } catch (MessagingException e) { log.warn("Could not close SMTP connection", e); }
            }
        }
    }

    private static final class MailConfig {
        private final String host;
        private final String port;
        private final String user;
        private final String password;
        private MailConfig(String host, String port, String user, String password) {
            this.host = host;
            this.port = port;
            this.user = user;
            this.password = password;
        }
    }
}
