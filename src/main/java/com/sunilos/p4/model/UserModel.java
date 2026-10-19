package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.UserBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.exception.RecordNotFoundException;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.EmailBuilder;
import com.sunilos.p4.util.EmailMessage;
import com.sunilos.p4.util.EmailUtility;
import com.sunilos.p4.util.JDBCDataSource;
import com.sunilos.p4.util.PasswordUtility;
import com.sunilos.p4.util.PropertyReader;

/**
 * JDBC Implementation of UserModel (Phase 2 Security Remediation).
 * - Hashes passwords via PBKDF2-HMAC-SHA-256 with unique random salt per password
 * - Transparently migrates legacy password hashes upon successful login
 * - Rejects locked accounts (UserBean.ACTIVE) during authentication and locks
 *   accounts after MAX_FAILED_ATTEMPTS consecutive failures
 * - Implements expiring, single-use, hashed password-reset tokens in ST_PASSWORD_RESET_TOKEN
 * - Never sends passwords in emails
 */
public class UserModel extends BaseModel<UserBean> {

	private static Logger log = Logger.getLogger(UserModel.class);
	public static final int MAX_FAILED_ATTEMPTS = 5;

	public static boolean isLockedFlag(String lockValue) {
		return UserBean.ACTIVE.equalsIgnoreCase(lockValue) || "LOCKED".equalsIgnoreCase(lockValue)
				|| "true".equalsIgnoreCase(lockValue);
	}

	@Override
	public long add(UserBean bean) throws ApplicationException, DuplicateRecordException {
		log.debug("Model add Started");
		Connection conn = null;
		int pk = 0;

		UserBean existbean = findByLogin(bean.getLogin());
		if (existbean != null) {
			throw new DuplicateRecordException("Login Id already exists");
		}

		String hashedPassword = PasswordUtility.hashPassword(bean.getPassword());
		// The encoded value is retained separately for persistence; do not leave
		// a plaintext credential on the mutable request bean.
		bean.setPassword(null);
		bean.setConfirmPassword(null);
		if (DataValidator.isNull(bean.getLock())) {
			bean.setLock(UserBean.INACTIVE);
		}

		try {
			conn = JDBCDataSource.getConnection();
			pk = nextPK();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn
					.prepareStatement("INSERT INTO ST_USER VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			pstmt.setInt(1, pk);
			pstmt.setString(2, bean.getFirstName());
			pstmt.setString(3, bean.getLastName());
			pstmt.setString(4, bean.getLogin());
			pstmt.setString(5, hashedPassword);
			pstmt.setDate(6, bean.getDob() != null ? new java.sql.Date(bean.getDob().getTime()) : null);
			pstmt.setString(7, bean.getMobileNo());
			pstmt.setLong(8, bean.getRoleId());
			pstmt.setInt(9, bean.getUnSuccessfulLogin());
			pstmt.setString(10, bean.getGender());
			pstmt.setTimestamp(11, bean.getLastLogin());
			pstmt.setString(12, bean.getLock());
			pstmt.setString(13, bean.getRegisteredIP());
			pstmt.setString(14, bean.getLastLoginIP());
			pstmt.setString(15, bean.getCreatedBy());
			pstmt.setString(16, bean.getModifiedBy());
			pstmt.setTimestamp(17, bean.getCreatedDatetime());
			pstmt.setTimestamp(18, bean.getModifiedDatetime());
			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception : Exception in add User");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model add End");
		return pk;
	}

	public UserBean findByLogin(String login) throws ApplicationException {
		return findByUniqueColumn("LOGIN", login);
	}

	@Override
	public void update(UserBean bean) throws ApplicationException, DuplicateRecordException {
		log.debug("Model update Started");
		Connection conn = null;

		// Load the row by ID first. Looking up by the possibly changed login is
		// not sufficient for retaining the current password hash during profile edits.
		UserBean existingById = findByPK(bean.getId());
		if (existingById == null) {
			throw new ApplicationException("User record not found for ID: " + bean.getId());
		}

		UserBean beanExist = findByLogin(bean.getLogin());
		if (beanExist != null && beanExist.getId() != bean.getId()) {
			throw new DuplicateRecordException("LoginId is already exist");
		}

		// The user-edit form never receives/echoes a password hash. A blank password
		// means preserve the hash on the existing row; a supplied password is plain
		// text and is hashed exactly once here.
		String passwordToStore;
		if (bean.getPassword() == null || bean.getPassword().isEmpty()
				|| PasswordUtility.isHashed(bean.getPassword())) {
			// A bean loaded from the database may still carry its encoded credential.
			// Never hash an existing PBKDF2/legacy digest as though it were a new
			// plaintext password. User-entered passwords arrive in the same field but
			// will not match a recognized stored-hash format.
			passwordToStore = existingById.getPassword();
		} else {
			passwordToStore = PasswordUtility.hashPassword(bean.getPassword());
		}
		// Never retain plaintext or an old form value on the bean after preparing
		// the safe value that will be persisted.
		bean.setPassword(null);
		bean.setConfirmPassword(null);

		if (bean.getRoleId() <= 0) {
			bean.setRoleId(existingById.getRoleId());
		}
		// Authentication/security state is not editable through a generic profile
		// update. Preserve it from the database row; dedicated methods manage lock,
		// failure count, login time and IP fields. Otherwise an ordinary profile POST
		// could accidentally clear a lockout or reset brute-force counters.
		bean.setUnSuccessfulLogin(existingById.getUnSuccessfulLogin());
		bean.setLastLogin(existingById.getLastLogin());
		bean.setLock(DataValidator.isNotNull(existingById.getLock())
				? existingById.getLock()
				: UserBean.INACTIVE);
		bean.setRegisteredIP(existingById.getRegisteredIP());
		bean.setLastLoginIP(existingById.getLastLoginIP());

		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement(
					"UPDATE ST_USER SET FIRST_NAME=?,LAST_NAME=?,LOGIN=?,PASSWORD=?,DOB=?,MOBILE_NO=?,ROLE_ID=?,UNSUCCESSFUL_LOGIN=?,GENDER=?,LAST_LOGIN=?,USER_LOCK=?,REGISTERED_IP=?,LAST_LOGIN_IP=?,CREATED_BY=?,MODIFIED_BY=?,CREATED_DATETIME=?,MODIFIED_DATETIME=? WHERE ID=?");
			pstmt.setString(1, bean.getFirstName());
			pstmt.setString(2, bean.getLastName());
			pstmt.setString(3, bean.getLogin());
			pstmt.setString(4, passwordToStore);
			pstmt.setDate(5, bean.getDob() != null ? new java.sql.Date(bean.getDob().getTime()) : null);
			pstmt.setString(6, bean.getMobileNo());
			pstmt.setLong(7, bean.getRoleId());
			pstmt.setInt(8, bean.getUnSuccessfulLogin());
			pstmt.setString(9, bean.getGender());
			pstmt.setTimestamp(10, bean.getLastLogin());
			pstmt.setString(11, bean.getLock());
			pstmt.setString(12, bean.getRegisteredIP());
			pstmt.setString(13, bean.getLastLoginIP());
			pstmt.setString(14, bean.getCreatedBy());
			pstmt.setString(15, bean.getModifiedBy());
			pstmt.setTimestamp(16, bean.getCreatedDatetime());
			pstmt.setTimestamp(17, bean.getModifiedDatetime());
			pstmt.setLong(18, bean.getId());
			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in updating User ");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model update End");
	}

	/**
	 * Authenticates user (Phase 2 Tasks 1, 4, 7):
	 * - Rejects locked accounts (UserBean.ACTIVE)
	 * - Verifies password via PasswordUtility.verifyPassword
	 * - Transparently upgrades legacy SHA-256 hashes to PBKDF2WithHmacSHA256 on success
	 * - Increments unsuccessfulLogin and locks account after MAX_FAILED_ATTEMPTS failures
	 */
	public UserBean authenticate(String login, String password) throws ApplicationException {
		UserBean bean = findByLogin(login);
		if (bean == null) {
			return null;
		}

		if (isLockedFlag(bean.getLock())) {
			log.warn("Authentication rejected for locked account: " + login);
			return null;
		}

		// Plaintext database passwords are not accepted; they must use the controlled reset path.
		if (!PasswordUtility.isHashed(bean.getPassword())) {
			log.warn("Account requires controlled password reset before login: " + login);
			throw new ApplicationException(PropertyReader.getValue("error.password.reset.required"));
		}

		if (PasswordUtility.verifyPassword(password, bean.getPassword())) {
			String upgradedHash = null;
			if (PasswordUtility.needsRehash(bean.getPassword())
					|| PasswordUtility.needsPepperUpgrade(password, bean.getPassword())) {
				upgradedHash = PasswordUtility.hashPassword(password);
			}
			Timestamp lastLogin = new Timestamp(System.currentTimeMillis());
			try {
				persistSuccessfulAuthentication(bean.getId(), upgradedHash, lastLogin);
				bean.setUnSuccessfulLogin(0);
				bean.setLastLogin(lastLogin);
				if (upgradedHash != null) {
					bean.setPassword(upgradedHash);
				}
			} catch (Exception e) {
				// Authentication itself succeeded. A failed audit/counter write is logged,
				// but must never rewrite the stored password as a hash of its own hash.
				log.warn("Could not persist successful authentication state: " + e.getMessage());
			}
			return bean;
		}

		int failures = bean.getUnSuccessfulLogin() + 1;
		String lockState = failures >= MAX_FAILED_ATTEMPTS ? UserBean.ACTIVE : bean.getLock();
		try {
			persistFailedAuthentication(bean.getId(), failures, lockState);
			bean.setUnSuccessfulLogin(failures);
			bean.setLock(lockState);
		} catch (Exception e) {
			log.warn("Could not update failed login counter: " + e.getMessage());
		}
		return null;
	}

	/** Persists successful login state without passing a stored hash through update(). */
	private void persistSuccessfulAuthentication(long userId, String upgradedHash, Timestamp lastLogin)
			throws ApplicationException {
		String sql = (upgradedHash == null)
				? "UPDATE ST_USER SET UNSUCCESSFUL_LOGIN=0, LAST_LOGIN=?, MODIFIED_DATETIME=NOW() WHERE ID=?"
				: "UPDATE ST_USER SET PASSWORD=?, UNSUCCESSFUL_LOGIN=0, LAST_LOGIN=?, MODIFIED_DATETIME=NOW() WHERE ID=?";
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				int i = 1;
				if (upgradedHash != null) {
					pstmt.setString(i++, upgradedHash);
				}
				pstmt.setTimestamp(i++, lastLogin);
				pstmt.setLong(i, userId);
				if (pstmt.executeUpdate() != 1) {
					throw new ApplicationException("User record not found while saving login state");
				}
			}
		} catch (ApplicationException e) {
			throw e;
		} catch (Exception e) {
			log.error("Could not save successful authentication state", e);
			throw new ApplicationException("Could not save authentication state");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	/** Persists failed-login counters/lock state without modifying the stored password. */
	private void persistFailedAuthentication(long userId, int failures, String lockState)
			throws ApplicationException {
		String sql = "UPDATE ST_USER SET UNSUCCESSFUL_LOGIN=?, USER_LOCK=?, MODIFIED_DATETIME=NOW() WHERE ID=?";
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setInt(1, failures);
				pstmt.setString(2, lockState);
				pstmt.setLong(3, userId);
				if (pstmt.executeUpdate() != 1) {
					throw new ApplicationException("User record not found while saving failed-login state");
				}
			}
		} catch (ApplicationException e) {
			throw e;
		} catch (Exception e) {
			log.error("Could not save failed authentication state", e);
			throw new ApplicationException("Could not save failed authentication state");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}


	/** Locks an account without rewriting its password hash. */
	public boolean lock(String login) throws RecordNotFoundException, ApplicationException {
		log.debug("Service lock Started");
		UserBean existing = findByLogin(login);
		if (existing == null) {
			throw new RecordNotFoundException("LoginId not exist");
		}
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			try (PreparedStatement pstmt = conn.prepareStatement(
					"UPDATE ST_USER SET USER_LOCK=?, MODIFIED_DATETIME=NOW() WHERE ID=?")) {
				pstmt.setString(1, UserBean.ACTIVE);
				pstmt.setLong(2, existing.getId());
				return pstmt.executeUpdate() == 1;
			}
		} catch (Exception e) {
			log.error("Could not lock user account", e);
			throw new ApplicationException("Could not lock user account");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	public List<UserBean> getRoles(UserBean bean) throws ApplicationException {
		log.debug("Model get roles Started");
		String sql = "SELECT * FROM ST_USER WHERE ROLE_ID=?";
		Connection conn = null;
		List<UserBean> list = new ArrayList<>();
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, bean.getRoleId());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				list.add(populateBean(rs));
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			throw new ApplicationException("Exception : Exception in get roles");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model get roles End");
		return list;
	}

	public boolean changePassword(Long id, String oldPassword, String newPassword)
			throws RecordNotFoundException, ApplicationException {

		log.debug("model changePassword Started");
		if (!DataValidator.isPasswordLength(newPassword)) {
			throw new ApplicationException(PropertyReader.getValue("error.password", "New Password"));
		}

		UserBean beanExist = findByPK(id);
		if (beanExist != null && PasswordUtility.verifyPassword(oldPassword, beanExist.getPassword())) {
			beanExist.setPassword(newPassword);
			try {
				update(beanExist);
				beanExist.setPassword(null);
			} catch (DuplicateRecordException e) {
				throw new ApplicationException("Exception updating password");
			}
		} else {
			throw new RecordNotFoundException("Old Password is invalid");
		}

		try {
			HashMap<String, String> map = new HashMap<>();
			map.put("login", beanExist.getLogin());
			map.put("firstName", beanExist.getFirstName());
			map.put("lastName", beanExist.getLastName());

			String message = EmailBuilder.getChangePasswordMessage(map);
			EmailMessage msg = new EmailMessage();
			msg.setTo(beanExist.getLogin());
			msg.setSubject("ORS Password has been changed Successfully.");
			msg.setMessage(message);
			msg.setMessageType(EmailMessage.HTML_MSG);

			EmailUtility.sendMail(msg);
		} catch (Exception e) {
			log.warn("Email notification skipped: " + e.getMessage());
		}

		log.debug("Model changePassword End");
		return true;
	}

	public UserBean updateAccess(UserBean bean) throws ApplicationException {
		return null;
	}

	public long registerUser(UserBean bean) throws ApplicationException, DuplicateRecordException {
		log.debug("Model registerUser Started");
		if (!DataValidator.isPasswordLength(bean.getPassword())) {
			throw new ApplicationException(PropertyReader.getValue("error.password", "Password"));
		}
		long pk = add(bean);

		try {
			HashMap<String, String> map = new HashMap<>();
			map.put("login", bean.getLogin());
			map.put("firstName", bean.getFirstName());
			map.put("lastName", bean.getLastName());

			String message = EmailBuilder.getUserRegistrationMessage(map);
			EmailMessage msg = new EmailMessage();
			msg.setTo(bean.getLogin());
			msg.setSubject("Registration is successful for ORS Project Rays Technologies");
			msg.setMessage(message);
			msg.setMessageType(EmailMessage.HTML_MSG);
			EmailUtility.sendMail(msg);
		} catch (ApplicationException e) {
			log.warn("Email notification skipped: " + e.getMessage());
		}
		return pk;
	}

	/**
	 * Issues a cryptographically random, expiring (15-minute), single-use password
	 * reset token stored hashed in ST_PASSWORD_RESET_TOKEN (Phase 2 Task 6).
	 */
	public String issuePasswordResetToken(String login) throws ApplicationException, RecordNotFoundException {
		// Test SMTP configuration before account lookup so missing/invalid mail
		// credentials produce the same result for known and unknown logins.
		EmailUtility.verifySmtpConnection();
		UserBean userData = findByLogin(login);
		if (userData == null) {
			throw new RecordNotFoundException("Email ID does not exist!");
		}

		String rawToken = PasswordUtility.generateResetToken();
		String tokenHash = PasswordUtility.hashResetToken(rawToken);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		Timestamp expiresAt = new Timestamp(now.getTime() + PasswordUtility.RESET_TOKEN_TTL_MS);

		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);

			// Invalidate any prior unused reset tokens for this account
			try (PreparedStatement invalidatePstmt = conn.prepareStatement(
					"UPDATE ST_PASSWORD_RESET_TOKEN SET USED = 1 WHERE USER_ID = ? AND USED = 0")) {
				invalidatePstmt.setLong(1, userData.getId());
				invalidatePstmt.executeUpdate();
			}

			long nextTokenId = 1L;
			try (PreparedStatement pkStmt = conn.prepareStatement("SELECT MAX(ID) FROM ST_PASSWORD_RESET_TOKEN");
					ResultSet rs = pkStmt.executeQuery()) {
				if (rs.next()) {
					nextTokenId = rs.getLong(1) + 1L;
				}
			}

			try (PreparedStatement insertPstmt = conn.prepareStatement(
					"INSERT INTO ST_PASSWORD_RESET_TOKEN (ID, USER_ID, LOGIN, TOKEN_HASH, EXPIRES_AT, USED, CREATED_DATETIME) VALUES (?, ?, ?, ?, ?, 0, ?)")) {
				insertPstmt.setLong(1, nextTokenId);
				insertPstmt.setLong(2, userData.getId());
				insertPstmt.setString(3, userData.getLogin());
				insertPstmt.setString(4, tokenHash);
				insertPstmt.setTimestamp(5, expiresAt);
				insertPstmt.setTimestamp(6, now);
				insertPstmt.executeUpdate();
			}

			conn.commit();
		} catch (Exception e) {
			JDBCDataSource.rollBack(conn);
			log.error("Error persisting password reset token", e);
			throw new ApplicationException("Unable to generate password reset token");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}

		try {
			HashMap<String, String> map = new HashMap<>();
			map.put("login", userData.getLogin());
			map.put("firstName", userData.getFirstName());
			map.put("lastName", userData.getLastName());
			map.put("resetToken", rawToken);
			map.put("expiresMinutes", "15");
			String message = EmailBuilder.getForgetPasswordMessage(map);
			EmailMessage msg = new EmailMessage();
			msg.setTo(userData.getLogin());
			msg.setSubject("SUNRAYS ORS Password Reset Token");
			msg.setMessage(message);
			msg.setMessageType(EmailMessage.HTML_MSG);
			EmailUtility.sendMail(msg);
		} catch (ApplicationException e) {
			// Do not leave an active token that the user never received.
			Connection cleanupConn = null;
			try {
				cleanupConn = JDBCDataSource.getConnection();
				try (PreparedStatement invalidate = cleanupConn.prepareStatement(
						"UPDATE ST_PASSWORD_RESET_TOKEN SET USED = 1 WHERE TOKEN_HASH = ? AND USED = 0")) {
					invalidate.setString(1, tokenHash);
					invalidate.executeUpdate();
				}
			} catch (Exception cleanupError) {
				log.error("Could not invalidate a reset token after email delivery failed", cleanupError);
			} finally {
				JDBCDataSource.closeConnection(cleanupConn);
			}
			log.error("Password reset token was stored but email delivery failed", e);
			throw new ApplicationException("Password-reset email delivery failed. Verify SMTP settings and review the Tomcat log. The undelivered token was invalidated.");
		}

		return rawToken;
	}

	/**
	 * Validates and consumes a single-use, expiring password-reset token and
	 * updates the user's password with PBKDF2-HMAC-SHA-256 (Phase 2 Task 6).
	 */
	public boolean consumePasswordResetToken(String login, String rawToken, String newPassword)
			throws ApplicationException, RecordNotFoundException {
		if (DataValidator.isNull(login) || DataValidator.isNull(rawToken)) {
			throw new ApplicationException(PropertyReader.getValue("error.reset.token.invalid"));
		}
		if (!DataValidator.isPasswordLength(newPassword)) {
			throw new ApplicationException(PropertyReader.getValue("error.password", "New Password"));
		}

		String tokenHash = PasswordUtility.hashResetToken(rawToken);
		UserBean userData = findByLogin(login);
		if (userData == null) {
			throw new ApplicationException(PropertyReader.getValue("error.reset.token.invalid"));
		}

		Timestamp now = new Timestamp(System.currentTimeMillis());

		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);

			long tokenId = -1L;
			Timestamp expiresAt = null;
			int used = 1;

			try (PreparedStatement pstmt = conn.prepareStatement(
					"SELECT ID, EXPIRES_AT, USED FROM ST_PASSWORD_RESET_TOKEN WHERE USER_ID = ? AND TOKEN_HASH = ?")) {
				pstmt.setLong(1, userData.getId());
				pstmt.setString(2, tokenHash);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						tokenId = rs.getLong("ID");
						expiresAt = rs.getTimestamp("EXPIRES_AT");
						used = rs.getInt("USED");
					}
				}
			}

			if (tokenId <= 0 || used != 0 || expiresAt == null || expiresAt.before(now)) {
				JDBCDataSource.rollBack(conn);
				throw new ApplicationException(PropertyReader.getValue("error.reset.token.invalid"));
			}

			// Mark token as single-use consumed
			try (PreparedStatement markUsed = conn.prepareStatement(
					"UPDATE ST_PASSWORD_RESET_TOKEN SET USED = 1 WHERE ID = ? AND USED = 0")) {
				markUsed.setLong(1, tokenId);
				int updatedRows = markUsed.executeUpdate();
				if (updatedRows != 1) {
					JDBCDataSource.rollBack(conn);
					throw new ApplicationException(PropertyReader.getValue("error.reset.token.invalid"));
				}
			}

			// Update user password with PBKDF2-HMAC-SHA-256 and clear lock state
			String newPasswordHash = PasswordUtility.hashPassword(newPassword);
			try (PreparedStatement updateUser = conn.prepareStatement(
					"UPDATE ST_USER SET PASSWORD = ?, USER_LOCK = ?, UNSUCCESSFUL_LOGIN = 0, MODIFIED_DATETIME = NOW() WHERE ID = ?")) {
				updateUser.setString(1, newPasswordHash);
				updateUser.setString(2, UserBean.INACTIVE);
				updateUser.setLong(3, userData.getId());
				updateUser.executeUpdate();
			}

			conn.commit();
			return true;
		} catch (ApplicationException e) {
			throw e;
		} catch (Exception e) {
			JDBCDataSource.rollBack(conn);
			log.error("Error consuming password reset token", e);
			throw new ApplicationException("Exception in resetting password with token");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	public boolean resetPassword(UserBean bean) throws ApplicationException {
		if (bean == null || DataValidator.isNull(bean.getLogin())) {
			return false;
		}
		try {
			issuePasswordResetToken(bean.getLogin());
			return true;
		} catch (RecordNotFoundException e) {
			return false;
		}
	}

	/**
	 * Non-enumerating password reset request (Phase 2 Task 10):
	 * Issues a reset token if the account exists, or performs a dummy token hash
	 * and returns true without revealing whether the email is registered.
	 */
	public boolean requestPasswordResetNonEnumerating(String login) throws ApplicationException {
		try {
			issuePasswordResetToken(login);
		} catch (RecordNotFoundException ignored) {
			// Keep the response non-enumerating, but leave a safe server-side clue
			// that no email was expected because no matching account exists.
			log.info("Password-reset requested for an unregistered login; no token email was sent.");
			PasswordUtility.hashResetToken(PasswordUtility.generateResetToken());
		}
		return true;
	}

	public boolean forgetPassword(String login) throws ApplicationException, RecordNotFoundException {
		issuePasswordResetToken(login);
		return true;
	}

	@Override
	protected void buildWhereParameters(UserBean bean, StringBuilder sql, List<Object> params) {
		if (bean != null) {
			if (bean.getId() > 0) {
				sql.append(" AND ID = ?");
				params.add(bean.getId());
			}
			if (bean.getFirstName() != null && !bean.getFirstName().isEmpty()) {
				sql.append(" AND FIRST_NAME LIKE ?");
				params.add(bean.getFirstName() + "%");
			}
			if (bean.getLastName() != null && !bean.getLastName().isEmpty()) {
				sql.append(" AND LAST_NAME LIKE ?");
				params.add(bean.getLastName() + "%");
			}
			if (bean.getLogin() != null && !bean.getLogin().isEmpty()) {
				sql.append(" AND LOGIN LIKE ?");
				params.add(bean.getLogin() + "%");
			}
			if (bean.getMobileNo() != null && !bean.getMobileNo().isEmpty()) {
				sql.append(" AND MOBILE_NO = ?");
				params.add(bean.getMobileNo());
			}
			if (bean.getRoleId() > 0) {
				sql.append(" AND ROLE_ID = ?");
				params.add(bean.getRoleId());
			}
			if (bean.getGender() != null && !bean.getGender().isEmpty()) {
				sql.append(" AND GENDER LIKE ?");
				params.add(bean.getGender() + "%");
			}
		}
	}

	@Override
	public String getTable() {
		return "ST_USER";
	}

	@Override
	public UserBean getBean() {
		return new UserBean();
	}

	@Override
	public String getWhereClause(UserBean bean) {
		StringBuilder sql = new StringBuilder();
		if (bean != null) {
			if (bean.getId() > 0) {
				sql.append(" AND ID = ").append(bean.getId());
			}
			if (bean.getRoleId() > 0) {
				sql.append(" AND ROLE_ID = ").append(bean.getRoleId());
			}
		}
		return sql.toString();
	}
}
