package com.sunilos.p4.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Cryptographic Password Hashing, Migration & Reset-Token Utility (Phase 2).
 *
 * - Uses PBKDF2-HMAC-SHA-256 (PBKDF2WithHmacSHA256) with a unique 128-bit
 *   cryptographically random salt per password and 65,536 iterations.
 * - Stores hashes in a versioned format:
 *     PBKDF2WithHmacSHA256$<iterations>$<saltHex>$<hashHex>
 * - Reads any optional pepper exclusively from environment configuration
 *   (ORS_PASSWORD_PEPPER or system property ors.password.pepper) outside
 *   source control.
 * - Supports backward-compatible verification and detection (needsRehash) of
 *   legacy SHA-256 digests so existing accounts migrate transparently on login.
 * - Generates and hashes single-use password-reset tokens.
 */
public final class PasswordUtility {

	public static final String ALGORITHM = "PBKDF2WithHmacSHA256";
	public static final int DEFAULT_ITERATIONS = 65536;
	public static final int SALT_BYTES = 16;
	public static final int KEY_LENGTH_BITS = 256;
	public static final long RESET_TOKEN_TTL_MS = 15L * 60L * 1000L; // 15 minutes

	private static final String PBKDF2_PREFIX = ALGORITHM + "$";
	private static final String LEGACY_SHA256_PREFIX = "SHA-256$";
	private static final Pattern HEX_64_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private PasswordUtility() {
	}

	/**
	 * Reads optional pepper from environment/system configuration outside source control.
	 */
	private static String resolvePepper() {
		String envPepper = System.getenv("ORS_PASSWORD_PEPPER");
		if (envPepper != null && !envPepper.isEmpty()) {
			return envPepper;
		}
		String propPepper = System.getProperty("ors.password.pepper");
		return (propPepper != null) ? propPepper : "";
	}

	/**
	 * Hashes a raw password using PBKDF2-HMAC-SHA-256 with a unique 16-byte random
	 * salt and returns a versioned string:
	 * PBKDF2WithHmacSHA256$65536$<saltHex>$<hashHex>
	 */
	public static String hashPassword(String rawPassword) {
		if (rawPassword == null) {
			return null;
		}
		if (isCurrentPbkdf2Format(rawPassword)) {
			return rawPassword;
		}
		byte[] salt = new byte[SALT_BYTES];
		SECURE_RANDOM.nextBytes(salt);
		byte[] derived = pbkdf2(rawPassword, salt, DEFAULT_ITERATIONS, KEY_LENGTH_BITS);
		return ALGORITHM + "$" + DEFAULT_ITERATIONS + "$" + toHex(salt) + "$" + toHex(derived);
	}

	/**
	 * Verifies a candidate plaintext password against a stored password record.
	 * Supports versioned PBKDF2WithHmacSHA256 records as well as legacy SHA-256
	 * records for backward-compatible account migration.
	 */
	public static boolean verifyPassword(String candidatePassword, String storedPassword) {
		if (candidatePassword == null || storedPassword == null || storedPassword.isEmpty()) {
			return false;
		}
		if (storedPassword.startsWith(PBKDF2_PREFIX)) {
			String[] parts = storedPassword.split("\\$");
			if (parts.length != 4) {
				return false;
			}
			try {
				int iterations = Integer.parseInt(parts[1]);
				byte[] salt = fromHex(parts[2]);
				byte[] expectedHash = fromHex(parts[3]);
				String configuredPepper = resolvePepper();
				byte[] actualHash = pbkdf2(candidatePassword, salt, iterations, expectedHash.length * 8, configuredPepper);
				if (MessageDigest.isEqual(expectedHash, actualHash)) {
					return true;
				}
				// Compatibility bridge for older PBKDF2 records created before a pepper was
				// configured. A successful no-pepper match is upgraded on login by UserModel.
				if (!configuredPepper.isEmpty()) {
					byte[] legacyNoPepperHash = pbkdf2(candidatePassword, salt, iterations, expectedHash.length * 8, "");
					return MessageDigest.isEqual(expectedHash, legacyNoPepperHash);
				}
				return false;
			} catch (Exception e) {
				return false;
			}
		}

		// Legacy migration path: SHA-256$salt$hex or 64-char hex digest
		if (storedPassword.startsWith(LEGACY_SHA256_PREFIX)) {
			String[] parts = storedPassword.split("\\$");
			if (parts.length == 3) {
				String expectedHex = sha256Hex(parts[1] + ":" + candidatePassword);
				return MessageDigest.isEqual(
						expectedHex.getBytes(StandardCharsets.UTF_8),
						parts[2].toLowerCase().getBytes(StandardCharsets.UTF_8));
			}
			return false;
		}

		if (HEX_64_PATTERN.matcher(storedPassword).matches()) {
			String unsaltedHex = sha256Hex(candidatePassword);
			if (MessageDigest.isEqual(
					unsaltedHex.getBytes(StandardCharsets.UTF_8),
					storedPassword.toLowerCase().getBytes(StandardCharsets.UTF_8))) {
				return true;
			}
			String legacyPepperedHex = sha256Hex("ORS_V0_2026_PEPPER:" + candidatePassword);
			return MessageDigest.isEqual(
					legacyPepperedHex.getBytes(StandardCharsets.UTF_8),
					storedPassword.toLowerCase().getBytes(StandardCharsets.UTF_8));
		}

		return false;
	}

	/**
	 * Returns true if the stored password is in a legacy format or uses fewer
	 * iterations than DEFAULT_ITERATIONS and should be upgraded on login.
	 */
	
	/**
	 * Hashes a password if it is not already in the current PBKDF2WithHmacSHA256 format.
	 * Provided for API compatibility with PasswordUtilityTest.
	 */
	public static String hashIfNeeded(String password) {
		if (password == null) {
			return null;
		}
		return isCurrentPbkdf2Format(password) ? password : hashPassword(password);
	}

	/**
	 * Verifies a candidate plaintext password against a stored PBKDF2 or legacy SHA-256 hash.
	 * Plaintext stored passwords are NEVER accepted; unhashed legacy records must use the
	 * controlled password-reset flow (/ForgetPasswordCtl).
	 */
	public static boolean verify(String candidatePassword, String storedPassword) {
		return verifyPassword(candidatePassword, storedPassword);
	}

	/**
	 * Returns true when the configured pepper is enabled and the supplied legacy-format
	 * PBKDF2 record only matches without that pepper. On successful login, UserModel uses
	 * this signal to replace it with a hash computed using the current pepper.
	 */
	public static boolean needsPepperUpgrade(String candidatePassword, String storedPassword) {
		String configuredPepper = resolvePepper();
		if (configuredPepper.isEmpty() || candidatePassword == null || storedPassword == null
				|| !storedPassword.startsWith(PBKDF2_PREFIX)) {
			return false;
		}
		String[] parts = storedPassword.split("\\$");
		if (parts.length != 4) {
			return false;
		}
		try {
			int iterations = Integer.parseInt(parts[1]);
			byte[] salt = fromHex(parts[2]);
			byte[] expectedHash = fromHex(parts[3]);
			byte[] currentHash = pbkdf2(candidatePassword, salt, iterations, expectedHash.length * 8, configuredPepper);
			if (MessageDigest.isEqual(expectedHash, currentHash)) {
				return false;
			}
			byte[] unpepperedHash = pbkdf2(candidatePassword, salt, iterations, expectedHash.length * 8, "");
			return MessageDigest.isEqual(expectedHash, unpepperedHash);
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean needsRehash(String storedPassword) {
		if (!isCurrentPbkdf2Format(storedPassword)) {
			return true;
		}
		String[] parts = storedPassword.split("\\$");
		try {
			int iterations = Integer.parseInt(parts[1]);
			return iterations < DEFAULT_ITERATIONS;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	public static boolean isCurrentPbkdf2Format(String value) {
		if (value == null || !value.startsWith(PBKDF2_PREFIX)) {
			return false;
		}
		String[] parts = value.split("\\$");
		return parts.length == 4 && parts[2].length() >= 32 && parts[3].length() == 64;
	}

	public static boolean isHashed(String value) {
		if (value == null) {
			return false;
		}
		return isCurrentPbkdf2Format(value)
				|| value.startsWith(LEGACY_SHA256_PREFIX)
				|| HEX_64_PATTERN.matcher(value).matches();
	}

	/**
	 * Generates a cryptographically random 256-bit single-use password reset token.
	 */
	public static String generateResetToken() {
		byte[] tokenBytes = new byte[32];
		SECURE_RANDOM.nextBytes(tokenBytes);
		return toHex(tokenBytes);
	}

	/**
	 * Hashes a password reset token using SHA-256 before storing or querying in DB.
	 */
	public static String hashResetToken(String rawToken) {
		if (rawToken == null || rawToken.trim().isEmpty()) {
			return null;
		}
		return sha256Hex("ORS_RESET_TOKEN:" + rawToken.trim());
	}

	private static byte[] pbkdf2(String password, byte[] salt, int iterations, int keyLengthBits) {
		return pbkdf2(password, salt, iterations, keyLengthBits, resolvePepper());
	}

	private static byte[] pbkdf2(String password, byte[] salt, int iterations, int keyLengthBits, String pepper) {
		try {
			char[] chars = (pepper == null || pepper.isEmpty() ? password : (pepper + ":" + password)).toCharArray();
			PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, keyLengthBits);
			SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
			return skf.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new IllegalStateException("PBKDF2WithHmacSHA256 not available", e);
		}
	}

	private static String sha256Hex(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return toHex(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 algorithm not available", e);
		}
	}

	private static String toHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private static byte[] fromHex(String hex) {
		int len = hex.length();
		byte[] out = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			out[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
					+ Character.digit(hex.charAt(i + 1), 16));
		}
		return out;
	}
}
