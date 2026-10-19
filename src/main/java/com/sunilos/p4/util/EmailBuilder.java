package com.sunilos.p4.util;

import java.util.HashMap;

/**
 * EmailBuilder constructs HTML notifications for registration, password reset
 * tokens, and password changes.
 * Phase 2 Task 5: Never includes plaintext or generated passwords in any email.
 */
public class EmailBuilder {

	public static String getUserRegistrationMessage(HashMap<String, String> map) {
		StringBuilder msg = new StringBuilder();
		String login = map != null ? map.getOrDefault("login", "") : "";
		String firstName = map != null ? map.getOrDefault("firstName", "") : "";
		String lastName = map != null ? map.getOrDefault("lastName", "") : "";

		msg.append("<HTML><BODY>");
		msg.append("<H2>Registration Confirmed — Online Result System (ORS)</H2>");
		msg.append("<P>Dear ").append(firstName).append(" ").append(lastName).append(",</P>");
		msg.append("<P>Your ORS account (<b>").append(login)
				.append("</b>) has been registered. For your security, passwords are never sent by email.</P>");
		msg.append("</BODY></HTML>");
		return msg.toString();
	}

	public static String getForgetPasswordMessage(HashMap<String, String> map) {
		StringBuilder msg = new StringBuilder();
		String firstName = map != null ? map.getOrDefault("firstName", "") : "";
		String lastName = map != null ? map.getOrDefault("lastName", "") : "";
		String login = map != null ? map.getOrDefault("login", "") : "";
		String resetToken = map != null ? map.getOrDefault("resetToken", "") : "";
		String expiresMinutes = map != null ? map.getOrDefault("expiresMinutes", "15") : "15";

		msg.append("<HTML><BODY>");
		msg.append("<H2>ORS Password Reset Request</H2>");
		msg.append("<P>Dear ").append(firstName).append(" ").append(lastName).append(" (").append(login)
				.append("),</P>");
		msg.append("<P>A password reset was requested for your account. For security, ORS never sends passwords via email.</P>");
		if (resetToken != null && !resetToken.isEmpty()) {
			msg.append("<P>Use the following single-use reset token within <b>")
					.append(expiresMinutes)
					.append(" minutes</b> on the Forget Password page to set a new 8-character password:</P>");
			msg.append("<P><code>").append(resetToken).append("</code></P>");
		}
		msg.append("</BODY></HTML>");
		return msg.toString();
	}

	public static String getChangePasswordMessage(HashMap<String, String> map) {
		StringBuilder msg = new StringBuilder();
		String firstName = map != null ? map.getOrDefault("firstName", "") : "";
		String lastName = map != null ? map.getOrDefault("lastName", "") : "";
		String login = map != null ? map.getOrDefault("login", "") : "";

		msg.append("<HTML><BODY>");
		msg.append("<H2>Your ORS Password Has Been Updated</H2>");
		msg.append("<P>Dear ").append(firstName).append(" ").append(lastName).append(" (").append(login)
				.append("),</P>");
		msg.append("<P>Your account password was changed. For security, your password is never included in email notifications.</P>");
		msg.append("</BODY></HTML>");
		return msg.toString();
	}
}
