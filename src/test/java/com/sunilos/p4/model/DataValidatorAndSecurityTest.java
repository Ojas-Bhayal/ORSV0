package com.sunilos.p4.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunilos.p4.bean.MarksheetBean;
import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.ctl.FrontController;
import com.sunilos.p4.ctl.GetMarksheetCtl;
import com.sunilos.p4.ctl.MarksheetMeritListCtl;
import com.sunilos.p4.ctl.PerformanceMonitorFilter;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.EmailBuilder;
import com.sunilos.p4.util.PasswordUtility;

/**
 * JUnit 5 Regression Test Suite for Phase 2, Phase 3, Phase 5, and Phase 6:
 * - PBKDF2-HMAC-SHA-256 with unique random salts per password
 * - Legacy SHA-256 verification and needsRehash migration detection
 * - Single-use password reset token generation and hashing
 * - Strict letters-only name, 8-char password, 10-digit mobile (6-9), email, and marks validation
 * - Student ID-only ownership and peer anonymization (rejecting same-name spoofing)
 * - Web & REST Role-Based Access Control matrix
 */
class DataValidatorAndSecurityTest {

	@Test
	void testStrictLettersOnlyNameValidation() {
		assertTrue(DataValidator.isName("Rahul"));
		assertTrue(DataValidator.isName("Manish"));
		assertFalse(DataValidator.isName("Manish Sharma"));
		assertFalse(DataValidator.isName("Rahul123"));
		assertFalse(DataValidator.isName("J.I.Y."));
		assertFalse(DataValidator.isName(""));
		assertFalse(DataValidator.isName(null));
	}

	@Test
	void testPasswordLengthValidation() {
		assertTrue(DataValidator.isPasswordLength("Admin123"));
		assertFalse(DataValidator.isPasswordLength("short"));
		assertFalse(DataValidator.isPasswordLength("tooLongPassword123"));
	}

	@Test
	void testPhoneValidation() {
		assertTrue(DataValidator.isPhoneNo("9826012345"));
		assertTrue(DataValidator.isPhoneNo("8871012345"));
		assertTrue(DataValidator.isPhoneNo("7896321452"));
		assertTrue(DataValidator.isPhoneNo("6261098765"));
		assertFalse(DataValidator.isPhoneNo("5261098765"));
		assertFalse(DataValidator.isPhoneNo("1234567890"));
		assertFalse(DataValidator.isPhoneNo("982601234"));
		assertFalse(DataValidator.isPhoneNo("98260123456"));
	}

	@Test
	void testEmailAndMarksValidation() {
		assertTrue(DataValidator.isEmail("manish@gmail.com"));
		assertFalse(DataValidator.isEmail("manishgmail.com"));
		assertFalse(DataValidator.isEmail("manish@.com"));

		assertTrue(DataValidator.isValidMarks("0"));
		assertTrue(DataValidator.isValidMarks("95"));
		assertTrue(DataValidator.isValidMarks("100"));
		assertFalse(DataValidator.isValidMarks("-1"));
		assertFalse(DataValidator.isValidMarks("101"));
		assertFalse(DataValidator.isValidMarks("abc"));
	}

	@Test
	void testPbkdf2UniqueSaltsAndLegacyMigration() {
		String raw = "Admin123";
		String hash1 = PasswordUtility.hashPassword(raw);
		String hash2 = PasswordUtility.hashPassword(raw);

		// Phase 2 Acceptance Criterion: Accounts with the same password receive different salted hashes
		assertNotEquals(hash1, hash2);
		assertTrue(hash1.startsWith("PBKDF2WithHmacSHA256$65536$"));
		assertTrue(PasswordUtility.isCurrentPbkdf2Format(hash1));
		assertFalse(PasswordUtility.needsRehash(hash1));
		assertTrue(PasswordUtility.verifyPassword("Admin123", hash1));
		assertTrue(PasswordUtility.verifyPassword("Admin123", hash2));
		assertFalse(PasswordUtility.verifyPassword("Wrong123", hash1));

		// Legacy SHA-256 hash should verify and be flagged for migration via needsRehash()
		String legacyHex = "2f5ea4324099529ab2c0fed2378056a8a48478b7cc18eb68516398e86c78e4d9";
		assertTrue(PasswordUtility.verifyPassword("Admin123", legacyHex));
		assertTrue(PasswordUtility.needsRehash(legacyHex));
	}

	@Test
	void testResetTokenGenerationAndHashing() {
		String token1 = PasswordUtility.generateResetToken();
		String token2 = PasswordUtility.generateResetToken();
		assertEquals(64, token1.length());
		assertNotEquals(token1, token2);

		String hash1 = PasswordUtility.hashResetToken(token1);
		assertEquals(64, hash1.length());
		assertNotEquals(token1, hash1);
		assertEquals(hash1, PasswordUtility.hashResetToken(token1));
	}

	@Test
	void testUserBeanJsonDoesNotSerializePassword() throws Exception {
		UserBean user = new UserBean();
		user.setId(1L);
		user.setFirstName("Anshul");
		user.setLastName("Prajapati");
		user.setLogin("an@gmail.com");
		user.setPassword(PasswordUtility.hashPassword("Admin123"));
		user.setConfirmPassword("Admin123");

		ObjectMapper mapper = new ObjectMapper();
		String json = mapper.writeValueAsString(user);

		assertFalse(json.contains("Admin123"));
		assertFalse(json.contains("PBKDF2WithHmacSHA256"));
		assertFalse(json.contains("\"password\""));
		assertFalse(json.contains("\"confirmPassword\""));
	}

	@Test
	void testEmailBuilderNeverIncludesPlaintextPassword() {
		HashMap<String, String> params = new HashMap<>();
		params.put("login", "manish@gmail.com");
		params.put("password", "Stud1234");
		params.put("firstName", "Manish");
		params.put("lastName", "Sharma");
		params.put("resetToken", "a1b2c3d4");

		String regEmail = EmailBuilder.getUserRegistrationMessage(params);
		String forgetEmail = EmailBuilder.getForgetPasswordMessage(params);
		String changeEmail = EmailBuilder.getChangePasswordMessage(params);

		assertFalse(regEmail.contains("Stud1234"));
		assertFalse(forgetEmail.contains("Stud1234"));
		assertFalse(changeEmail.contains("Stud1234"));
		assertTrue(forgetEmail.contains("a1b2c3d4"));
	}

	@Test
	void testStudentIdOwnershipAndPeerAnonymizationRejectsSameNameSpoofing() {
		MarksheetBean ownRow = new MarksheetBean();
		ownRow.setStudentId(2L);
		ownRow.setRollNo("1");
		ownRow.setName("Manish Sharma");
		ownRow.setMarks(95);

		// Another student with the EXACT same display name but a different Student ID (99L)
		MarksheetBean sameNamePeerRow = new MarksheetBean();
		sameNamePeerRow.setStudentId(99L);
		sameNamePeerRow.setRollNo("99");
		sameNamePeerRow.setName("Manish Sharma");
		sameNamePeerRow.setMarks(88);

		assertTrue(GetMarksheetCtl.isOwnedByStudentId(2L, List.of(ownRow)));
		assertFalse(GetMarksheetCtl.isOwnedByStudentId(2L, List.of(sameNamePeerRow)),
				"Same display name with different Student ID must be rejected!");

		List<MarksheetBean> rawList = new ArrayList<>();
		rawList.add(ownRow);
		rawList.add(sameNamePeerRow);

		List<MarksheetBean> anonymized = MarksheetMeritListCtl.anonymizePeerRecordsByStudentId(2L, rawList);

		assertEquals("Manish Sharma", anonymized.get(0).getName());
		assertEquals("1", anonymized.get(0).getRollNo());
		assertEquals(MarksheetMeritListCtl.ANONYMOUS_PEER_NAME, anonymized.get(1).getName(),
				"Peer with same name but different Student ID must be anonymized!");
		assertEquals(MarksheetMeritListCtl.HIDDEN_ROLL_NO, anonymized.get(1).getRollNo());
	}

	@Test
	void testRBACAuthorizationMatrixForWebAndRest() {
		assertTrue(FrontController.isAuthorized(RoleBean.ADMIN, "/ORSV0/ctl/UserCtl", "POST", false));
		assertTrue(FrontController.isAuthorized(RoleBean.ADMIN, "/ORSV0/ctl/EnrollmentCtl", "POST", false));
		assertTrue(FrontController.isAuthorized(RoleBean.ADMIN, "/ORSV0/ctl/FacultyAllocationCtl", "POST", false));
		assertTrue(FrontController.isAuthorized(RoleBean.ADMIN, "/ORSV0/rest/UserRestCtl/get/1", "GET", true));

		assertFalse(FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/ctl/UserCtl", "GET", false));
		assertFalse(FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/ctl/AuditCtl", "GET", false));
		assertFalse(FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/ctl/EnrollmentListCtl", "GET", false));
		assertFalse(FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/ctl/MarksheetCtl", "POST", false));
		assertFalse(FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/rest/UserRestCtl/get/1", "GET", true));
		assertFalse(FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/rest/rolectl/get/1", "GET", true));
		assertTrue(FrontController.isAuthorized(RoleBean.STUDENT, "/ORSV0/ctl/GetMarksheetCtl", "GET", false));

		assertTrue(FrontController.isAuthorized(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/AuditCtl", "GET", false));
		assertTrue(FrontController.isAuthorized(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/EnrollmentListCtl", "GET", false));
		assertTrue(FrontController.isAuthorized(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/FacultyAllocationListCtl", "GET", false));
		assertTrue(FrontController.isAuthorized(RoleBean.PROGRAM_CHAIR, "/ORSV0/rest/studentctl/get/1", "GET", true));
		assertFalse(FrontController.isAuthorized(RoleBean.PROGRAM_CHAIR, "/ORSV0/ctl/UserCtl", "POST", false));
		assertFalse(FrontController.isAuthorized(RoleBean.PROGRAM_CHAIR, "/ORSV0/rest/UserRestCtl/get/1", "GET", true));

		assertTrue(FrontController.isAuthorized(RoleBean.FACULTY, "/ORSV0/ctl/StudentListCtl", "GET", false));
		assertTrue(FrontController.isAuthorized(RoleBean.FACULTY, "/ORSV0/ctl/MarksheetCtl", "POST", false));
		assertFalse(FrontController.isAuthorized(RoleBean.FACULTY, "/ORSV0/ctl/AuditCtl", "GET", false));
		assertFalse(FrontController.isAuthorized(RoleBean.FACULTY, "/ORSV0/ctl/UserCtl", "POST", false));
	}

	@Test
	void testSlaThresholdsMatchExperiment2() {
		assertEquals(3000L, PerformanceMonitorFilter.resolveThresholdMs("/ORSV0/ctl/GetMarksheetCtl", "POST"));
		assertEquals(2000L, PerformanceMonitorFilter.resolveThresholdMs("/ORSV0/ctl/MarksheetCtl", "POST"));
		assertEquals(1500L, PerformanceMonitorFilter.resolveThresholdMs("/ORSV0/WelcomeCtl", "GET"));
	}

	@Test
	void testTenMinuteSessionInactivityTimeoutConfiguration() throws Exception {
		assertEquals("10", com.sunilos.p4.util.PropertyReader.getValue("session.timeout.minutes"));
		java.nio.file.Path webXml = java.nio.file.Paths.get("src/main/webapp/WEB-INF/web.xml");
		if (java.nio.file.Files.exists(webXml)) {
			String xml = java.nio.file.Files.readString(webXml);
			assertTrue(xml.contains("<session-timeout>10</session-timeout>"));
		}
	}
}
