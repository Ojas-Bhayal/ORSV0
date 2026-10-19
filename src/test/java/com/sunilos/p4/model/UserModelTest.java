package com.sunilos.p4.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.util.JDBCDataSource;
import com.sunilos.p4.util.PasswordUtility;

/**
 * Live Relational Database Test Suite for UserModel (Phase 2 & Phase 4):
 * - PBKDF2-HMAC-SHA-256 hashing on add()
 * - Transparent legacy SHA-256 upgrade on authenticate()
 * - Plaintext stored password rejection requiring controlled reset (/ForgetPasswordCtl)
 * - 5-strike brute-force account lockout and locked-account login rejection
 * - Non-enumerating password reset and 15-minute single-use hashed reset tokens
 */
class UserModelTest {

	@AfterEach
	void clearTestConnectionSupplier() {
		JDBCDataSource.clearTestConnectionSupplier();
	}

	@BeforeEach
	void setUpDatabase() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_URL", "jdbc:hsqldb:mem:ors_user_test_db;sql.syntax_mys=true");
		JDBCDataSource.setTestConnectionSupplier(() -> DriverManager.getConnection(
				"jdbc:hsqldb:mem:ors_user_test_db;sql.syntax_mys=true", "sa", ""));
		System.setProperty("ORS_DB_DRIVER", "org.hsqldb.jdbcDriver");
		System.setProperty("ORS_DB_USER", "sa");
		System.setProperty("ORS_DB_PASSWORD", "");
		JDBCDataSource.closeConnection(null);

		try (Connection conn = JDBCDataSource.getConnection(); Statement st = conn.createStatement()) {
			st.execute("DROP TABLE ST_PASSWORD_RESET_TOKEN IF EXISTS");
			st.execute("DROP TABLE ST_USER IF EXISTS");
			st.execute("DROP TABLE ST_ROLE IF EXISTS");
			st.execute("CREATE TABLE ST_ROLE (ID BIGINT PRIMARY KEY, NAME VARCHAR(100), DESCRIPTION VARCHAR(500), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
			st.execute("CREATE TABLE ST_USER (ID BIGINT PRIMARY KEY, FIRST_NAME VARCHAR(50), LAST_NAME VARCHAR(50), LOGIN VARCHAR(100), PASSWORD VARCHAR(255), DOB DATE, MOBILE_NO VARCHAR(20), ROLE_ID BIGINT, UNSUCCESSFUL_LOGIN INT, GENDER VARCHAR(20), LAST_LOGIN TIMESTAMP, USER_LOCK VARCHAR(20), REGISTERED_IP VARCHAR(50), LAST_LOGIN_IP VARCHAR(50), CREATED_BY VARCHAR(50), MODIFIED_BY VARCHAR(50), CREATED_DATETIME TIMESTAMP, MODIFIED_DATETIME TIMESTAMP)");
			st.execute("CREATE TABLE ST_PASSWORD_RESET_TOKEN (ID BIGINT PRIMARY KEY, USER_ID BIGINT, LOGIN VARCHAR(100), TOKEN_HASH VARCHAR(128), EXPIRES_AT TIMESTAMP, USED INT DEFAULT 0, CREATED_DATETIME TIMESTAMP)");
			conn.commit();
		}
	}

	private static String sha256Hex(String input) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		for (byte b : digest) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	@Test
	void testAuthenticateUpgradesLegacySha256AndRejectsPlaintext() throws Exception {
		UserModel model = new UserModel();
		UserBean user = new UserBean();
		user.setFirstName("Aarav");
		user.setLastName("Sharma");
		user.setLogin("aarav.user@gmail.com");
		user.setPassword("Pass1234");
		user.setRoleId(RoleBean.STUDENT);
		user.setMobileNo("9826012345");
		user.setDob(new Date());
		long uid = model.add(user);

		UserBean stored = model.findByPK(uid);
		assertTrue(PasswordUtility.isCurrentPbkdf2Format(stored.getPassword()));

		// Simulate legacy SHA-256 digest in DB and verify transparent upgrade on login
		try (Connection conn = JDBCDataSource.getConnection();
				PreparedStatement ps = conn.prepareStatement("UPDATE ST_USER SET PASSWORD = ? WHERE ID = ?")) {
			ps.setString(1, sha256Hex("Pass1234"));
			ps.setLong(2, uid);
			ps.executeUpdate();
			conn.commit();
		}
		assertNotNull(model.authenticate("aarav.user@gmail.com", "Pass1234"));
		String upgradedHash = model.findByPK(uid).getPassword();
		assertTrue(PasswordUtility.isCurrentPbkdf2Format(upgradedHash));
		// The rehashed credential must still verify on the next login; hashing an
		// already-hashed value would make the user unable to log in again.
		assertNotNull(model.authenticate("aarav.user@gmail.com", "Pass1234"));
		assertTrue(PasswordUtility.verifyPassword("Pass1234", model.findByPK(uid).getPassword()));

		// Simulate unhashed plaintext password in DB and verify controlled reset exception is thrown
		try (Connection conn = JDBCDataSource.getConnection();
				PreparedStatement ps = conn.prepareStatement("UPDATE ST_USER SET PASSWORD = ? WHERE ID = ?")) {
			ps.setString(1, "Pass1234");
			ps.setLong(2, uid);
			ps.executeUpdate();
			conn.commit();
		}
		assertThrows(ApplicationException.class, () -> model.authenticate("aarav.user@gmail.com", "Pass1234"));
	}

	@Test
	void testAccountLockoutAndSingleUsePasswordResetToken() throws Exception {
		UserModel model = new UserModel();
		UserBean user = new UserBean();
		user.setFirstName("Diya");
		user.setLastName("Patel");
		user.setLogin("diya.user@gmail.com");
		user.setPassword("Pass1234");
		user.setRoleId(RoleBean.STUDENT);
		user.setMobileNo("9826012346");
		user.setDob(new Date());
		long uid = model.add(user);

		String originalHash = model.findByPK(uid).getPassword();
		for (int i = 0; i < 5; i++) {
			assertNull(model.authenticate("diya.user@gmail.com", "Wrong123"));
			assertTrue(originalHash.equals(model.findByPK(uid).getPassword()),
					"A failed login must not change or re-hash the stored password");
		}
		assertTrue(UserBean.ACTIVE.equalsIgnoreCase(model.findByPK(uid).getLock()));
		assertNull(model.authenticate("diya.user@gmail.com", "Pass1234"), "Locked account must be rejected");

		// A generic profile update must not unlock the account or reset its counter.
		UserBean profileEdit = model.findByPK(uid);
		profileEdit.setLock(UserBean.INACTIVE);
		profileEdit.setUnSuccessfulLogin(0);
		profileEdit.setLastLogin(null);
		profileEdit.setPassword("");
		model.update(profileEdit);
		UserBean afterProfileEdit = model.findByPK(uid);
		assertTrue(UserBean.ACTIVE.equalsIgnoreCase(afterProfileEdit.getLock()),
				"A profile edit must not clear the lock flag");
		assertEquals(5, afterProfileEdit.getUnSuccessfulLogin(),
				"A profile edit must not reset failed-login attempts");

		// Non-enumerating reset request returns true for unknown email too
		assertTrue(model.requestPasswordResetNonEnumerating("unknown.account@gmail.com"));

		String token = model.issuePasswordResetToken("diya.user@gmail.com");
		assertNotNull(token);
		assertTrue(model.consumePasswordResetToken("diya.user@gmail.com", token, "NewPass8"));
		assertThrows(ApplicationException.class,
				() -> model.consumePasswordResetToken("diya.user@gmail.com", token, "Reuse999"));
		assertNotNull(model.authenticate("diya.user@gmail.com", "NewPass8"));
	}

	@Test
	void testChangePasswordAndProfileUpdatePreserveValidPasswordHash() throws Exception {
		UserModel model = new UserModel();
		UserBean user = new UserBean();
		user.setFirstName("Aarav");
		user.setLastName("Sharma");
		user.setLogin("change.password@gmail.com");
		user.setPassword("Pass1234");
		user.setRoleId(RoleBean.STUDENT);
		user.setMobileNo("9826012345");
		user.setDob(new Date());
		long id = model.add(user);

		assertTrue(model.changePassword(id, "Pass1234", "Changed8"));
		assertNotNull(model.authenticate("change.password@gmail.com", "Changed8"),
				"Changed password must be hashed once and remain usable");
		assertNull(model.authenticate("change.password@gmail.com", "Pass1234"));

		UserBean editable = model.findByPK(id);
		String beforeLoginChange = editable.getPassword();
		editable.setLogin("renamed.account@gmail.com");
		// Simulate an internal caller that obtained the bean from the model and did
		// not clear the already encoded value. update() must not hash this hash again.
		model.update(editable);
		UserBean afterUpdate = model.findByLogin("renamed.account@gmail.com");
		assertNotNull(afterUpdate);
		assertEquals(beforeLoginChange, afterUpdate.getPassword(),
				"Changing the login must preserve the stored hash and never hash a loaded digest again");
		assertNotNull(model.authenticate("renamed.account@gmail.com", "Changed8"));

		String beforeLock = model.findByPK(id).getPassword();
		assertTrue(model.lock("renamed.account@gmail.com"));
		assertEquals(beforeLock, model.findByPK(id).getPassword(),
				"Locking an account must not modify the password hash");
	}

}
