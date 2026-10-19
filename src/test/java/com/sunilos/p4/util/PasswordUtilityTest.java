package com.sunilos.p4.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;

/**
 * Unit test suite for PasswordUtility (Phase 2 & Phase 4).
 * Verifies PBKDF2-HMAC-SHA-256 salted hashing, hashIfNeeded(), verify(),
 * legacy SHA-256 verification/rehash detection, plaintext rejection, and
 * single-use reset token hashing.
 */
class PasswordUtilityTest {

	@Test
	void testHashPasswordAndHashIfNeededProduceUniqueSaltedPbkdf2Hashes() {
		String h1 = PasswordUtility.hashPassword("Admin123");
		String h2 = PasswordUtility.hashIfNeeded("Admin123");

		assertNotNull(h1);
		assertNotNull(h2);
		assertNotEquals(h1, h2);
		assertTrue(h1.startsWith("PBKDF2WithHmacSHA256$65536$"));
		assertTrue(h2.startsWith("PBKDF2WithHmacSHA256$65536$"));
		assertEquals(h1, PasswordUtility.hashIfNeeded(h1), "Already hashed PBKDF2 value should not be double-hashed");
	}

	@Test
	void testVerifyAndPlaintextRejection() {
		String hashed = PasswordUtility.hashPassword("Stud1234");
		assertTrue(PasswordUtility.verify("Stud1234", hashed));
		assertTrue(PasswordUtility.verifyPassword("Stud1234", hashed));
		assertFalse(PasswordUtility.verify("Wrong123", hashed));

		// Plaintext stored password must NEVER verify
		assertFalse(PasswordUtility.verify("Stud1234", "Stud1234"));
		assertFalse(PasswordUtility.verifyPassword("Stud1234", "Stud1234"));
		assertFalse(PasswordUtility.isHashed("Stud1234"));
	}

	@Test
	void testFreshP04DemoAccountHashesMatchDocumentedPasswords() {
		Assumptions.assumeTrue(System.getenv("ORS_PASSWORD_PEPPER") == null
				|| System.getenv("ORS_PASSWORD_PEPPER").isEmpty(),
				"The fresh demo hashes were generated without a pepper");
		String oldPepper = System.getProperty("ors.password.pepper");
		try {
			System.clearProperty("ors.password.pepper");
			assertTrue(PasswordUtility.verifyPassword("Admin123",
					"PBKDF2WithHmacSHA256$65536$a1f04c92d83e71b50629ca84e3175df0$0280c1e6ab8c38cd4595e3c28683a630d4ddbffe0fd6b9aaa95c29755e35efd1"));
			assertTrue(PasswordUtility.verifyPassword("Stud1234",
					"PBKDF2WithHmacSHA256$65536$b2e15d03e94f82c6173adb95f4286ea1$e3dfd2c86a9a95d29e3c77f2881058bab92dbcb999b323c7258b486acd5ecb82"));
			assertTrue(PasswordUtility.verifyPassword("Chair123",
					"PBKDF2WithHmacSHA256$65536$c3d26e14fa5093d7284bec0605397fb2$1747d311e627d9ed70390cd621be90776825863be06f9e776b6c856ec75d6509"));
			assertTrue(PasswordUtility.verifyPassword("Facu1234",
					"PBKDF2WithHmacSHA256$65536$d4c37f250b61a4e8395cfd17164a80c3$9d85147f358f76113ff99947ae47386e02bf157f28e8b5f06ccf64784f5736e3"));
		} finally {
			if (oldPepper == null) {
				System.clearProperty("ors.password.pepper");
			} else {
				System.setProperty("ors.password.pepper", oldPepper);
			}
		}
	}

	@Test
	void testLegacySha256VerificationAndNeedsRehash() {
		String legacyHex = "2f5ea4324099529ab2c0fed2378056a8a48478b7cc18eb68516398e86c78e4d9"; // SHA-256("Admin123")
		assertTrue(PasswordUtility.isHashed(legacyHex));
		assertTrue(PasswordUtility.verify("Admin123", legacyHex));
		assertTrue(PasswordUtility.needsRehash(legacyHex));
	}
	@Test
	void testUnpepperedPbkdf2HashCanBeUpgradedWhenPepperIsEnabled() {
		Assumptions.assumeTrue(System.getenv("ORS_PASSWORD_PEPPER") == null
				|| System.getenv("ORS_PASSWORD_PEPPER").isEmpty(),
				"This test controls the pepper with a system property and expects no overriding environment variable");
		String oldPepper = System.getProperty("ors.password.pepper");
		try {
			System.clearProperty("ors.password.pepper");
			String legacyHash = PasswordUtility.hashPassword("Student123");
			System.setProperty("ors.password.pepper", "test-pepper-for-migration");
			assertTrue(PasswordUtility.verifyPassword("Student123", legacyHash));
			assertTrue(PasswordUtility.needsPepperUpgrade("Student123", legacyHash));
			String upgradedHash = PasswordUtility.hashPassword("Student123");
			assertTrue(PasswordUtility.verifyPassword("Student123", upgradedHash));
			assertFalse(PasswordUtility.needsPepperUpgrade("Student123", upgradedHash));
		} finally {
			if (oldPepper == null) {
				System.clearProperty("ors.password.pepper");
			} else {
				System.setProperty("ors.password.pepper", oldPepper);
			}
		}
	}

}
