package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.EnrollmentBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.JDBCDataSource;

/**
 * EnrollmentModel manages Student-to-Course enrollments in ST_ENROLLMENT
 * (Phase 4 Database Relationships & Integrity).
 *
 * - Treats active ST_ENROLLMENT rows (STATUS='ACTIVE') as the sole authoritative
 *   source of enrollment (no fallback to ST_STUDENT.COURSE_ID).
 * - Provides transactional reconciliation when a student's course assignment
 *   changes so old relationships are deactivated and failures roll back cleanly.
 * - Verifies that a Roll Number belongs to the selected Student ID.
 */
public class EnrollmentModel extends BaseModel<EnrollmentBean> {

	private static Logger log = Logger.getLogger(EnrollmentModel.class);

	private void resolveNames(EnrollmentBean bean) throws ApplicationException {
		if (bean.getStudentId() > 0 && (DataValidator.isNull(bean.getStudentName()) || DataValidator.isNull(bean.getRollNo()))) {
			StudentModel sModel = new StudentModel();
			StudentBean student = sModel.findByPK(bean.getStudentId());
			if (student != null) {
				if (DataValidator.isNull(bean.getStudentName())) {
					bean.setStudentName((student.getFirstName() + " " + student.getLastName()).trim());
				}
			}
		}
		if (bean.getCourseId() > 0 && DataValidator.isNull(bean.getCourseName())) {
			CourseModel cModel = new CourseModel();
			CourseBean course = cModel.findByPK(bean.getCourseId());
			if (course != null) {
				bean.setCourseName(course.getName());
			}
		}
		if (DataValidator.isNull(bean.getRollNo()) && bean.getStudentId() > 0) {
			// Only reuse a previously stored marksheet roll number if it maps to this
			// student/course. Do not invent a roll number from the database ID; legacy
			// data can contain conflicting roll numbers and must be reviewed explicitly.
			String rollNo = lookupMarksheetRollNo(bean.getStudentId(), bean.getCourseId());
			if (DataValidator.isNotNull(rollNo)) {
				bean.setRollNo(rollNo);
			}
		}
		if (DataValidator.isNull(bean.getSemester())) {
			bean.setSemester("Sem 1");
		}
		if (DataValidator.isNull(bean.getStatus())) {
			bean.setStatus("ACTIVE");
		}
	}

	/**
	 * Older enrollment rows can have empty denormalized ROLL_NO values. Reuse the
	 * authoritative marksheet roll number when present; otherwise callers display
	 * the student ID as a safe last-resort identifier.
	 */
	private String lookupMarksheetRollNo(long studentId, long courseId) {
		if (studentId <= 0) return null;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			String sql = "SELECT ROLL_NO FROM ST_MARKSHEET WHERE STUDENT_ID = ?"
					+ (courseId > 0 ? " AND COURSE_ID = ?" : "")
					+ " AND ROLL_NO IS NOT NULL AND TRIM(ROLL_NO) <> '' ORDER BY ID ASC LIMIT 1";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setLong(1, studentId);
				if (courseId > 0) pstmt.setLong(2, courseId);
				try (ResultSet rs = pstmt.executeQuery()) {
					return rs.next() ? rs.getString(1) : null;
				}
			}
		} catch (Exception e) {
			log.warn("Could not resolve legacy enrollment roll number from marksheets", e);
			return null;
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	@Override
	protected EnrollmentBean populateBean(ResultSet rs) {
		EnrollmentBean bean = super.populateBean(rs);
		try {
			resolveNames(bean);
		} catch (ApplicationException e) {
			log.warn("Unable to resolve display names for enrollment row " + bean.getId(), e);
		}
		return bean;
	}

	@Override
	public long add(EnrollmentBean bean) throws ApplicationException, DuplicateRecordException {
		resolveNames(bean);
		checkDuplicate(bean);

		String columns = "ID,STUDENT_ID,STUDENT_NAME,ROLL_NO,COURSE_ID,COURSE_NAME,SEMESTER,STATUS";
		String values = "?,?,?,?,?,?,?,?";
		StringBuilder sql = new StringBuilder("INSERT INTO " + getTable());
		sql.append("(CREATED_DATETIME,MODIFIED_DATETIME,CREATED_BY,MODIFIED_BY," + columns + ")");
		sql.append(" VALUES(NOW(),NOW(),?,?, " + values + ")");

		Connection conn = null;
		int pk = 0;
		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			pk = nextPK();
			String actor = DataValidator.isNotNull(bean.getCreatedBy()) ? bean.getCreatedBy() : getDefaultUser();
			PreparedStatement pstmt = conn.prepareStatement(sql.toString());
			pstmt.setString(1, actor);
			pstmt.setString(2, actor);
			pstmt.setInt(3, pk);
			pstmt.setLong(4, bean.getStudentId());
			pstmt.setString(5, bean.getStudentName());
			pstmt.setString(6, bean.getRollNo());
			pstmt.setLong(7, bean.getCourseId());
			pstmt.setString(8, bean.getCourseName());
			pstmt.setString(9, bean.getSemester());
			pstmt.setString(10, bean.getStatus());
			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();
		} catch (SQLException e) {
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in adding Enrollment: " + e.getMessage());
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return pk;
	}

	@Override
	public void update(EnrollmentBean bean) throws ApplicationException, DuplicateRecordException {
		resolveNames(bean);
		checkDuplicate(bean);

		String sql = "UPDATE " + getTable()
				+ " SET STUDENT_ID=?,STUDENT_NAME=?,ROLL_NO=?,COURSE_ID=?,COURSE_NAME=?,SEMESTER=?,STATUS=? WHERE ID=?";
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, bean.getStudentId());
			pstmt.setString(2, bean.getStudentName());
			pstmt.setString(3, bean.getRollNo());
			pstmt.setLong(4, bean.getCourseId());
			pstmt.setString(5, bean.getCourseName());
			pstmt.setString(6, bean.getSemester());
			pstmt.setString(7, bean.getStatus());
			pstmt.setLong(8, bean.getId());
			pstmt.executeUpdate();
			updatedTimestamp(bean.getId(), conn);
			conn.commit();
			pstmt.close();
		} catch (SQLException e) {
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in updating Enrollment: " + e.getMessage());
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	/**
	 * Transactional enrollment reconciliation executed on the caller's active
	 * JDBC Connection (Phase 4 Tasks 3, 4, 5):
	 * - Deactivates obsolete primary course enrollments when a student's primary
	 *   assigned course changes.
	 * - Inserts or reactivates the active enrollment row for (studentId, courseId).
	 * - Propagates SQLException so the caller rolls back the entire transaction.
	 */
	public void reconcileStudentEnrollment(Connection conn, long studentId, StudentBean studentBean,
			long previousCourseId) throws SQLException {
		if (studentId <= 0 || studentBean == null || studentBean.getCourseId() <= 0) {
			throw new SQLException("Invalid student or course ID for enrollment reconciliation");
		}

		long newCourseId = studentBean.getCourseId();
		String fullName = (studentBean.getFirstName() + " " + studentBean.getLastName()).trim();
		String actor = DataValidator.isNotNull(studentBean.getModifiedBy()) ? studentBean.getModifiedBy()
				: getDefaultUser();

		// If the student's primary course changed, deactivate the previous course enrollment
		if (previousCourseId > 0 && previousCourseId != newCourseId) {
			try (PreparedStatement deactivateStmt = conn.prepareStatement(
					"UPDATE ST_ENROLLMENT SET STATUS='INACTIVE', MODIFIED_BY=?, MODIFIED_DATETIME=NOW() WHERE STUDENT_ID=? AND COURSE_ID=?")) {
				deactivateStmt.setString(1, actor);
				deactivateStmt.setLong(2, studentId);
				deactivateStmt.setLong(3, previousCourseId);
				deactivateStmt.executeUpdate();
			}
		}

		// Check if an enrollment record already exists for (studentId, newCourseId)
		long existingEnrollmentId = -1L;
		try (PreparedStatement checkStmt = conn.prepareStatement(
				"SELECT ID FROM ST_ENROLLMENT WHERE STUDENT_ID=? AND COURSE_ID=?")) {
			checkStmt.setLong(1, studentId);
			checkStmt.setLong(2, newCourseId);
			try (ResultSet rs = checkStmt.executeQuery()) {
				if (rs.next()) {
					existingEnrollmentId = rs.getLong(1);
				}
			}
		}

		if (existingEnrollmentId > 0) {
			try (PreparedStatement reactivateStmt = conn.prepareStatement(
					"UPDATE ST_ENROLLMENT SET STUDENT_NAME=?, COURSE_NAME=?, STATUS='ACTIVE', MODIFIED_BY=?, MODIFIED_DATETIME=NOW() WHERE ID=?")) {
				reactivateStmt.setString(1, fullName);
				reactivateStmt.setString(2, studentBean.getCourseName());
				reactivateStmt.setString(3, actor);
				reactivateStmt.setLong(4, existingEnrollmentId);
				reactivateStmt.executeUpdate();
			}
		} else {
			long nextId = 1L;
			try (PreparedStatement pkStmt = conn.prepareStatement("SELECT MAX(ID) FROM ST_ENROLLMENT");
					ResultSet rs = pkStmt.executeQuery()) {
				if (rs.next()) {
					nextId = rs.getLong(1) + 1L;
				}
			}
			String defaultRollNo = String.valueOf(studentId);
			try (PreparedStatement rollStmt = conn.prepareStatement(
					"SELECT ROLL_NO FROM ST_ENROLLMENT WHERE STUDENT_ID=? ORDER BY ID ASC LIMIT 1")) {
				rollStmt.setLong(1, studentId);
				try (ResultSet rs = rollStmt.executeQuery()) {
					if (rs.next() && DataValidator.isNotNull(rs.getString(1))) {
						defaultRollNo = rs.getString(1);
					}
				}
			}

			try (PreparedStatement insertStmt = conn.prepareStatement(
					"INSERT INTO ST_ENROLLMENT (ID,STUDENT_ID,STUDENT_NAME,ROLL_NO,COURSE_ID,COURSE_NAME,SEMESTER,STATUS,CREATED_BY,MODIFIED_BY,CREATED_DATETIME,MODIFIED_DATETIME) VALUES (?,?,?,?,?,?,'Sem 1','ACTIVE',?,?,NOW(),NOW())")) {
				insertStmt.setLong(1, nextId);
				insertStmt.setLong(2, studentId);
				insertStmt.setString(3, fullName);
				insertStmt.setString(4, defaultRollNo);
				insertStmt.setLong(5, newCourseId);
				insertStmt.setString(6, studentBean.getCourseName());
				insertStmt.setString(7, actor);
				insertStmt.setString(8, actor);
				insertStmt.executeUpdate();
			}
		}
	}

	@Override
	public void checkDuplicate(EnrollmentBean bean) {
		EnrollmentBean duplicate = findByStudentAndCourse(bean.getStudentId(), bean.getCourseId());
		if (duplicate != null && duplicate.getId() != bean.getId()) {
			throw new DuplicateRecordException("Student is already enrolled in this course");
		}
		if (bean.getId() == 0 && duplicate != null) {
			throw new DuplicateRecordException("Student is already enrolled in this course");
		}
	}

	public EnrollmentBean findByStudentAndCourse(long studentId, long courseId) throws ApplicationException {
		String sql = "SELECT * FROM ST_ENROLLMENT WHERE STUDENT_ID=? AND COURSE_ID=?";
		EnrollmentBean bean = null;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, studentId);
			pstmt.setLong(2, courseId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				bean = populateBean(rs);
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception in findByStudentAndCourse", e);
			throw new ApplicationException("Exception in getting Enrollment by Student and Course");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return bean;
	}

	/**
	 * Returns only ACTIVE enrollments for the given studentId ordered deterministically
	 * by COURSE_ID ASC, ID ASC (Spec 04: Student Benchmark Active Enrollment).
	 */
	public List<EnrollmentBean> findActiveByStudentId(long studentId) throws ApplicationException {
		List<EnrollmentBean> activeList = new java.util.ArrayList<>();
		if (studentId <= 0) {
			return activeList;
		}
		String sql = "SELECT * FROM ST_ENROLLMENT WHERE STUDENT_ID = ? AND STATUS = 'ACTIVE' ORDER BY COURSE_ID ASC, ID ASC";
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setLong(1, studentId);
				try (ResultSet rs = pstmt.executeQuery()) {
					while (rs.next()) {
						activeList.add(populateBean(rs));
					}
				}
			}
		} catch (Exception e) {
			log.error("Database Exception in findActiveByStudentId", e);
			throw new ApplicationException("Exception in getting active enrollments by studentId");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return activeList;
	}

	public List<EnrollmentBean> findByStudentId(long studentId) throws ApplicationException {
		EnrollmentBean searchBean = new EnrollmentBean();
		searchBean.setStudentId(studentId);
		searchBean.setStatus("ACTIVE");
		return search(searchBean, 0, 0);
	}

	public List<EnrollmentBean> findByCourseId(long courseId) throws ApplicationException {
		EnrollmentBean searchBean = new EnrollmentBean();
		searchBean.setCourseId(courseId);
		searchBean.setStatus("ACTIVE");
		return search(searchBean, 0, 0);
	}

	/**
	 * Phase 4 Tasks 1 & 2: Active ST_ENROLLMENT records (STATUS='ACTIVE') are the
	 * SOLE authoritative source for enrollment checks. No fallback to ST_STUDENT.COURSE_ID.
	 */
	public boolean isStudentEnrolledInCourse(long studentId, long courseId) throws ApplicationException {
		if (studentId <= 0 || courseId <= 0) {
			return false;
		}
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(
					"SELECT COUNT(*) FROM ST_ENROLLMENT WHERE STUDENT_ID=? AND COURSE_ID=? AND STATUS='ACTIVE'");
			pstmt.setLong(1, studentId);
			pstmt.setLong(2, courseId);
			ResultSet rs = pstmt.executeQuery();
			boolean enrolled = rs.next() && rs.getInt(1) > 0;
			rs.close();
			pstmt.close();
			return enrolled;
		} catch (Exception e) {
			log.error("Error checking authoritative student course enrollment", e);
			return false;
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	public boolean isStudentEnrolledInAnyCourse(long studentId, Set<Long> courseIds) {
		if (studentId <= 0 || courseIds == null || courseIds.isEmpty()) {
			return false;
		}
		for (Long cid : courseIds) {
			if (cid != null && cid > 0 && isStudentEnrolledInCourse(studentId, cid)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Phase 4 Task 7: Verifies that the supplied rollNo belongs to the selected
	 * studentId in their active ST_ENROLLMENT record for courseId, and is not
	 * owned by any other student.
	 */
	public boolean verifyRollNoBelongsToStudent(long studentId, long courseId, String rollNo) {
		if (studentId <= 0 || courseId <= 0 || DataValidator.isNull(rollNo)) {
			return false;
		}
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			String trimmedRoll = rollNo.trim();

			// 1. Check active enrollment for (studentId, courseId)
			String enrolledRoll = null;
			try (PreparedStatement pstmt = conn.prepareStatement(
					"SELECT ROLL_NO FROM ST_ENROLLMENT WHERE STUDENT_ID=? AND COURSE_ID=? AND STATUS='ACTIVE'")) {
				pstmt.setLong(1, studentId);
				pstmt.setLong(2, courseId);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						enrolledRoll = rs.getString(1);
					} else {
						return false;
					}
				}
			}
			// The active enrollment is the authoritative source of the roll number.
			// Do not accept an arbitrary roll number when the enrollment has not been assigned one.
			if (enrolledRoll == null || enrolledRoll.trim().isEmpty()) {
				return false;
			}
			if (!enrolledRoll.trim().equalsIgnoreCase(trimmedRoll)) {
				return false;
			}

			// 2. Ensure no other student owns this rollNo in ST_ENROLLMENT or ST_MARKSHEET
			try (PreparedStatement conflictStmt = conn.prepareStatement(
					"SELECT COUNT(*) FROM ST_ENROLLMENT WHERE ROLL_NO=? AND STUDENT_ID<>?")) {
				conflictStmt.setString(1, trimmedRoll);
				conflictStmt.setLong(2, studentId);
				try (ResultSet rs = conflictStmt.executeQuery()) {
					if (rs.next() && rs.getInt(1) > 0) {
						return false;
					}
				}
			}
			return true;
		} catch (Exception e) {
			log.error("Roll number ownership check failed closed", e);
			return false;
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	/** Counts distinct active students for a course inside one college. */
	public int getEnrollmentCountByCourseAndCollege(long courseId, long collegeId) throws ApplicationException {
		if (courseId <= 0 || collegeId <= 0) {
			throw new ApplicationException("A valid course and college are required for scoped enrollment counts");
		}
		String sql = "SELECT COUNT(DISTINCT e.STUDENT_ID) FROM ST_ENROLLMENT e "
				+ "INNER JOIN ST_STUDENT s ON s.ID=e.STUDENT_ID "
				+ "WHERE e.COURSE_ID=? AND UPPER(e.STATUS)='ACTIVE' AND s.COLLEGE_ID=?";
		try (Connection conn = JDBCDataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, courseId);
			stmt.setLong(2, collegeId);
			try (ResultSet rs = stmt.executeQuery()) {
				return rs.next() ? rs.getInt(1) : 0;
			}
		} catch (Exception e) {
			log.error("Unable to count active enrollments for course " + courseId + " in college " + collegeId, e);
			throw new ApplicationException("Exception in computing scoped enrollment count");
		}
	}

	public int getEnrollmentCountByCourse(long courseId) {
		if (courseId <= 0) {
			return 0;
		}
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(
					"SELECT COUNT(DISTINCT STUDENT_ID) FROM ST_ENROLLMENT WHERE COURSE_ID=? AND STATUS='ACTIVE'");
			pstmt.setLong(1, courseId);
			ResultSet rs = pstmt.executeQuery();
			int count = 0;
			if (rs.next()) {
				count = rs.getInt(1);
			}
			rs.close();
			pstmt.close();
			return count;
		} catch (Exception e) {
			log.error("Error computing enrollment headcount for course " + courseId, e);
			return 0;
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	@Override
	protected void buildWhereParameters(EnrollmentBean bean, StringBuilder whereSql, List<Object> params) {
		if (bean == null) {
			return;
		}
		if (bean.getId() > 0) {
			whereSql.append(" AND ID = ?");
			params.add(bean.getId());
		}
		if (bean.getStudentId() > 0) {
			whereSql.append(" AND STUDENT_ID = ?");
			params.add(bean.getStudentId());
		}
		if (bean.getScopedStudentId() != null && bean.getScopedStudentId() > 0) {
			whereSql.append(" AND STUDENT_ID = ?");
			params.add(bean.getScopedStudentId());
		}
		if (bean.getCourseId() > 0) {
			whereSql.append(" AND COURSE_ID = ?");
			params.add(bean.getCourseId());
		}
		if (bean.getScopedCourseIds() != null && !bean.getScopedCourseIds().isEmpty()) {
			whereSql.append(" AND COURSE_ID IN (");
			int idx = 0;
			for (Long cid : bean.getScopedCourseIds()) {
				if (idx++ > 0) {
					whereSql.append(",");
				}
				whereSql.append("?");
				params.add(cid);
			}
			whereSql.append(")");
		}
		if (bean.getScopedCollegeId() != null && bean.getScopedCollegeId() > 0) {
			whereSql.append(" AND STUDENT_ID IN (SELECT ID FROM ST_STUDENT WHERE COLLEGE_ID = ?)");
			params.add(bean.getScopedCollegeId());
		}
		if (DataValidator.isNotNull(bean.getRollNo())) {
			whereSql.append(" AND ROLL_NO = ?");
			params.add(bean.getRollNo().trim());
		}
		if (DataValidator.isNotNull(bean.getStatus())) {
			whereSql.append(" AND STATUS = ?");
			params.add(bean.getStatus().trim().toUpperCase());
		}
		if (DataValidator.isNotNull(bean.getStudentName())) {
			whereSql.append(" AND STUDENT_NAME LIKE ?");
			params.add(bean.getStudentName() + "%");
		}
		if (DataValidator.isNotNull(bean.getCourseName())) {
			whereSql.append(" AND COURSE_NAME LIKE ?");
			params.add(bean.getCourseName() + "%");
		}
	}

	@Override
	public String getWhereClause(EnrollmentBean bean) {
		StringBuilder sql = new StringBuilder();
		if (bean != null && bean.getId() > 0) {
			sql.append(" AND ID = ").append(bean.getId());
		}
		return sql.toString();
	}

	@Override
	public String getTable() {
		return "ST_ENROLLMENT";
	}

	@Override
	public EnrollmentBean getBean() {
		return new EnrollmentBean();
	}
}
