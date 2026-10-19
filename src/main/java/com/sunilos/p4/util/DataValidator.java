package com.sunilos.p4.util;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * Input validation utility for the Online Result System (ORS).
 * Enforces strict letters-only names, 8-character passwords, 10-digit Indian
 * mobile numbers (starting with 6-9), RFC-compliant emails, and 0-100 marks.
 *
 * @author Rays EdTech
 * @version 3.0
 * @Copyright (c) Rays EdTech
 */
public class DataValidator {

	/**
	 * Requirement 1 & Audit Section 4: Strictly letters only (A-Z, a-z), no spaces,
	 * no digits, no punctuation.
	 */
	private static final Pattern STRICT_LETTERS_PATTERN = Pattern.compile("^[A-Za-z]+$");

	/**
	 * Strict Phone/Mobile Pattern: Exactly 10 digits, starting with 6, 7, 8, or 9.
	 */
	private static final Pattern PHONE_PATTERN = Pattern.compile("^[6-9][0-9]{9}$");

	/**
	 * Strict Email Pattern: Standard RFC-compliant email validation.
	 */
	private static final Pattern EMAIL_PATTERN = Pattern.compile(
			"^[_A-Za-z0-9+\\-]+(\\.[_A-Za-z0-9+\\-]+)*@[A-Za-z0-9]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$");

	/**
	 * Safe SQL column identifier pattern.
	 */
	private static final Pattern SQL_IDENTIFIER_PATTERN = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

	/**
	 * Required password length (8 characters).
	 */
	public static final int REQUIRED_PASSWORD_LENGTH = 8;

	public static boolean isNull(String val) {
		return val == null || val.trim().isEmpty();
	}

	public static boolean isNotNull(String val) {
		return !isNull(val);
	}

	public static boolean isInteger(String val) {
		if (isNotNull(val)) {
			try {
				Integer.parseInt(val.trim());
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	public static boolean isLong(String val) {
		if (isNotNull(val)) {
			try {
				Long.parseLong(val.trim());
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return false;
	}

	/**
	 * Requirement 1: Strictly letters only (A-Z, a-z). Rejects spaces, digits, and
	 * special characters.
	 */
	public static boolean isName(String val) {
		return isNotNull(val) && STRICT_LETTERS_PATTERN.matcher(val.trim()).matches();
	}

	public static boolean isAlphaOnly(String val) {
		return isName(val);
	}

	/**
	 * Requirement 2: Validates that password is 8 characters long.
	 */
	public static boolean isPasswordLength(String val) {
		return isNotNull(val) && val.length() == REQUIRED_PASSWORD_LENGTH;
	}

	public static boolean isPassword(String val) {
		return isPasswordLength(val);
	}

	/**
	 * Requirement 3: Validates that a phone/mobile number is strictly 10 digits
	 * long and starts with 6, 7, 8, or 9.
	 */
	public static boolean isPhoneNo(String val) {
		return isNotNull(val) && PHONE_PATTERN.matcher(val.trim()).matches();
	}

	public static boolean isMobileNo(String val) {
		return isPhoneNo(val);
	}

	/**
	 * Requirement 5: Checks if value is a valid Email ID.
	 */
	public static boolean isEmail(String val) {
		return isNotNull(val) && EMAIL_PATTERN.matcher(val.trim()).matches();
	}

	/**
	 * Checks if value is a valid Date.
	 */
	public static boolean isDate(String val) {
		if (isNotNull(val)) {
			Date d = DataUtility.getDate(val.trim());
			return d != null;
		}
		return false;
	}

	/**
	 * Validates that marks are an integer between 0 and 100 inclusive.
	 */
	public static boolean isValidMarks(String val) {
		if (!isInteger(val)) {
			return false;
		}
		int marks = Integer.parseInt(val.trim());
		return marks >= 0 && marks <= 100;
	}

	/**
	 * Validates that a SQL column identifier contains only safe characters.
	 */
	public static boolean isSafeSqlIdentifier(String col) {
		return isNotNull(col) && SQL_IDENTIFIER_PATTERN.matcher(col.trim()).matches();
	}

}
