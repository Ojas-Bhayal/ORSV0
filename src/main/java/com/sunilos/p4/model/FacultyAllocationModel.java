package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.FacultyAllocationBean;
import com.sunilos.p4.bean.FacultyBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.JDBCDataSource;

/**
 * FacultyAllocationModel manages Faculty-to-Course/Subject teaching allocations
 * in ST_FACULTY_ALLOCATION with fail-closed scope checks and transactional
 * reconciliation (Phase 3 & Phase 4).
 */
public class FacultyAllocationModel extends BaseModel<FacultyAllocationBean> {

	private static Logger log = Logger.getLogger(FacultyAllocationModel.class);

	private void resolveMetadata(FacultyAllocationBean bean) throws ApplicationException {
		if (bean.getFacultyId() > 0
				&& (DataValidator.isNull(bean.getFacultyName()) || DataValidator.isNull(bean.getFacultyEmail()))) {
			FacultyModel fModel = new FacultyModel();
			FacultyBean faculty = fModel.findByPK(bean.getFacultyId());
			if (faculty != null) {
				bean.setFacultyName(faculty.getFirstName() + " " + faculty.getLastName());
				bean.setFacultyEmail(faculty.getEmail());
			}
		}
		if (bean.getCourseId() > 0 && DataValidator.isNull(bean.getCourseName())) {
			CourseModel cModel = new CourseModel();
			CourseBean course = cModel.findByPK(bean.getCourseId());
			if (course != null) {
				bean.setCourseName(course.getName());
			}
		}
		if (bean.getSubjectId() > 0 && DataValidator.isNull(bean.getSubjectName())) {
			SubjectModel sModel = new SubjectModel();
			SubjectBean subject = sModel.findByPK(bean.getSubjectId());
			if (subject != null) {
				bean.setSubjectName(subject.getName());
			}
		}
	}

	/**
	 * Existing allocation rows may predate the denormalized display-name fields.
	 * Resolve blank faculty/course/subject labels from their authoritative tables
	 * whenever rows are read so list pages and the departmental audit show names.
	 */
	@Override
	protected FacultyAllocationBean populateBean(ResultSet rs) {
		FacultyAllocationBean bean = super.populateBean(rs);
		try {
			resolveMetadata(bean);
		} catch (ApplicationException e) {
			log.warn("Unable to resolve display metadata for faculty allocation row " + bean.getId(), e);
		}
		return bean;
	}

	@Override
	public long add(FacultyAllocationBean bean) throws ApplicationException, DuplicateRecordException {
		resolveMetadata(bean);
		checkDuplicate(bean);

		String columns = "ID,FACULTY_ID,FACULTY_NAME,FACULTY_EMAIL,COURSE_ID,COURSE_NAME,SUBJECT_ID,SUBJECT_NAME";
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
			pstmt.setLong(4, bean.getFacultyId());
			pstmt.setString(5, bean.getFacultyName());
			pstmt.setString(6, bean.getFacultyEmail());
			pstmt.setLong(7, bean.getCourseId());
			pstmt.setString(8, bean.getCourseName());
			pstmt.setLong(9, bean.getSubjectId());
			pstmt.setString(10, bean.getSubjectName());
			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();
		} catch (SQLException e) {
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in adding FacultyAllocation: " + e.getMessage());
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return pk;
	}

	@Override
	public void update(FacultyAllocationBean bean) throws ApplicationException, DuplicateRecordException {
		resolveMetadata(bean);
		checkDuplicate(bean);

		String sql = "UPDATE " + getTable()
				+ " SET FACULTY_ID=?,FACULTY_NAME=?,FACULTY_EMAIL=?,COURSE_ID=?,COURSE_NAME=?,SUBJECT_ID=?,SUBJECT_NAME=? WHERE ID=?";
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, bean.getFacultyId());
			pstmt.setString(2, bean.getFacultyName());
			pstmt.setString(3, bean.getFacultyEmail());
			pstmt.setLong(4, bean.getCourseId());
			pstmt.setString(5, bean.getCourseName());
			pstmt.setLong(6, bean.getSubjectId());
			pstmt.setString(7, bean.getSubjectName());
			pstmt.setLong(8, bean.getId());
			pstmt.executeUpdate();
			updatedTimestamp(bean.getId(), conn);
			conn.commit();
			pstmt.close();
		} catch (SQLException e) {
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in updating FacultyAllocation: " + e.getMessage());
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	/**
	 * Transactional faculty-allocation reconciliation executed on the caller's
	 * active JDBC Connection (Phase 4 Tasks 3, 4, 5):
	 * - Removes or updates obsolete primary allocation row when a faculty member's
	 *   assigned course/subject changes.
	 * - Propagates SQLException so the caller rolls back the entire transaction.
	 */
	public void reconcileFacultyAllocation(Connection conn, long facultyId, FacultyBean facultyBean,
			long previousCourseId, long previousSubjectId) throws SQLException {
		if (facultyId <= 0 || facultyBean == null || facultyBean.getCourseId() <= 0
				|| facultyBean.getSubjectId() <= 0) {
			return;
		}

		long newCourseId = facultyBean.getCourseId();
		long newSubjectId = facultyBean.getSubjectId();
		String fullName = (facultyBean.getFirstName() + " " + facultyBean.getLastName()).trim();
		String actor = DataValidator.isNotNull(facultyBean.getModifiedBy()) ? facultyBean.getModifiedBy()
				: getDefaultUser();

		if (previousCourseId > 0 && previousSubjectId > 0
				&& (previousCourseId != newCourseId || previousSubjectId != newSubjectId)) {
			try (PreparedStatement deleteOld = conn.prepareStatement(
					"DELETE FROM ST_FACULTY_ALLOCATION WHERE FACULTY_ID=? AND COURSE_ID=? AND SUBJECT_ID=?")) {
				deleteOld.setLong(1, facultyId);
				deleteOld.setLong(2, previousCourseId);
				deleteOld.setLong(3, previousSubjectId);
				deleteOld.executeUpdate();
			}
		}

		long existingAllocId = -1L;
		try (PreparedStatement checkStmt = conn.prepareStatement(
				"SELECT ID FROM ST_FACULTY_ALLOCATION WHERE FACULTY_ID=? AND COURSE_ID=? AND SUBJECT_ID=?")) {
			checkStmt.setLong(1, facultyId);
			checkStmt.setLong(2, newCourseId);
			checkStmt.setLong(3, newSubjectId);
			try (ResultSet rs = checkStmt.executeQuery()) {
				if (rs.next()) {
					existingAllocId = rs.getLong(1);
				}
			}
		}

		if (existingAllocId > 0) {
			try (PreparedStatement updateStmt = conn.prepareStatement(
					"UPDATE ST_FACULTY_ALLOCATION SET FACULTY_NAME=?, FACULTY_EMAIL=?, COURSE_NAME=?, SUBJECT_NAME=?, MODIFIED_BY=?, MODIFIED_DATETIME=NOW() WHERE ID=?")) {
				updateStmt.setString(1, fullName);
				updateStmt.setString(2, facultyBean.getEmail());
				updateStmt.setString(3, facultyBean.getCourseName());
				updateStmt.setString(4, facultyBean.getSubjectName());
				updateStmt.setString(5, actor);
				updateStmt.setLong(6, existingAllocId);
				updateStmt.executeUpdate();
			}
		} else {
			long nextId = 1L;
			try (PreparedStatement pkStmt = conn.prepareStatement("SELECT MAX(ID) FROM ST_FACULTY_ALLOCATION");
					ResultSet rs = pkStmt.executeQuery()) {
				if (rs.next()) {
					nextId = rs.getLong(1) + 1L;
				}
			}
			try (PreparedStatement insertStmt = conn.prepareStatement(
					"INSERT INTO ST_FACULTY_ALLOCATION (ID,FACULTY_ID,FACULTY_NAME,FACULTY_EMAIL,COURSE_ID,COURSE_NAME,SUBJECT_ID,SUBJECT_NAME,CREATED_BY,MODIFIED_BY,CREATED_DATETIME,MODIFIED_DATETIME) VALUES (?,?,?,?,?,?,?,?,?,?,NOW(),NOW())")) {
				insertStmt.setLong(1, nextId);
				insertStmt.setLong(2, facultyId);
				insertStmt.setString(3, fullName);
				insertStmt.setString(4, facultyBean.getEmail());
				insertStmt.setLong(5, newCourseId);
				insertStmt.setString(6, facultyBean.getCourseName());
				insertStmt.setLong(7, newSubjectId);
				insertStmt.setString(8, facultyBean.getSubjectName());
				insertStmt.setString(9, actor);
				insertStmt.setString(10, actor);
				insertStmt.executeUpdate();
			}
		}
	}

	@Override
	public void checkDuplicate(FacultyAllocationBean bean) {
		FacultyAllocationBean duplicate = findByFacultyCourseSubject(bean.getFacultyId(), bean.getCourseId(),
				bean.getSubjectId());
		if (duplicate != null && duplicate.getId() != bean.getId()) {
			throw new DuplicateRecordException("Faculty is already allocated to this Course and Subject");
		}
		if (bean.getId() == 0 && duplicate != null) {
			throw new DuplicateRecordException("Faculty is already allocated to this Course and Subject");
		}
	}

	public FacultyAllocationBean findByFacultyCourseSubject(long facultyId, long courseId, long subjectId)
			throws ApplicationException {
		String sql = "SELECT * FROM ST_FACULTY_ALLOCATION WHERE FACULTY_ID=? AND COURSE_ID=? AND SUBJECT_ID=?";
		FacultyAllocationBean bean = null;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, facultyId);
			pstmt.setLong(2, courseId);
			pstmt.setLong(3, subjectId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				bean = populateBean(rs);
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception in findByFacultyCourseSubject", e);
			throw new ApplicationException("Exception in getting FacultyAllocation by Faculty, Course, and Subject");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return bean;
	}

	/**
	 * Loads all assignments for faculty on the current directory page. Optional college
	 * and course scopes are enforced inside SQL so the directory cannot leak assignments
	 * from another Program Chair's authorized data scope.
	 */
	public List<FacultyAllocationBean> findAllocationsForFacultyIds(Collection<Long> facultyIds,
			Collection<Long> scopedCourseIds, Long scopedCollegeId) throws ApplicationException {
		Set<Long> safeIds = new LinkedHashSet<>();
		if (facultyIds != null) {
			for (Long id : facultyIds) {
				if (id != null && id.longValue() > 0) safeIds.add(id);
			}
		}
		if (safeIds.isEmpty()) return java.util.Collections.emptyList();

		StringBuilder sql = new StringBuilder("SELECT * FROM ST_FACULTY_ALLOCATION WHERE FACULTY_ID IN (");
		appendPlaceholders(sql, safeIds.size());
		sql.append(")");
		List<Object> parameters = new ArrayList<>();
		parameters.addAll(safeIds);
		Set<Long> safeCourses = new LinkedHashSet<>();
		if (scopedCourseIds != null) {
			for (Long courseId : scopedCourseIds) {
				if (courseId != null && courseId.longValue() > 0) safeCourses.add(courseId);
			}
		}
		if (scopedCourseIds != null) {
			if (safeCourses.isEmpty()) return java.util.Collections.emptyList();
			sql.append(" AND COURSE_ID IN (");
			appendPlaceholders(sql, safeCourses.size());
			sql.append(")");
			parameters.addAll(safeCourses);
		}
		if (scopedCollegeId != null && scopedCollegeId.longValue() > 0) {
			sql.append(" AND FACULTY_ID IN (SELECT ID FROM ST_FACULTY WHERE COLLEGE_ID = ?)");
			parameters.add(scopedCollegeId);
		}
		sql.append(" ORDER BY FACULTY_ID, COURSE_ID, SUBJECT_ID, ID");

		List<FacultyAllocationBean> rows = new ArrayList<>();
		try (Connection conn = JDBCDataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
			for (int i = 0; i < parameters.size(); i++) stmt.setObject(i + 1, parameters.get(i));
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next()) rows.add(populateBean(rs));
			}
			return rows;
		} catch (Exception e) {
			log.error("Unable to load directory teaching assignments", e);
			throw new ApplicationException("Unable to load faculty teaching assignments");
		}
	}

	private static void appendPlaceholders(StringBuilder sql, int count) {
		for (int i = 0; i < count; i++) {
			if (i > 0) sql.append(',');
			sql.append('?');
		}
	}

	public List<FacultyAllocationBean> findByFacultyEmail(String email) throws ApplicationException {
		if (DataValidator.isNull(email)) {
			return java.util.Collections.emptyList();
		}
		FacultyAllocationBean searchBean = new FacultyAllocationBean();
		searchBean.setFacultyEmail(email);
		return search(searchBean, 0, 0);
	}

	public boolean isFacultyAllocatedByEmail(String email, long courseId, long subjectId) {
		if (DataValidator.isNull(email) || courseId <= 0) {
			return false;
		}
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			String sql = subjectId > 0
					? "SELECT COUNT(*) FROM ST_FACULTY_ALLOCATION WHERE LOWER(FACULTY_EMAIL)=? AND COURSE_ID=? AND SUBJECT_ID=?"
					: "SELECT COUNT(*) FROM ST_FACULTY_ALLOCATION WHERE LOWER(FACULTY_EMAIL)=? AND COURSE_ID=?";
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, email.trim().toLowerCase());
			pstmt.setLong(2, courseId);
			if (subjectId > 0) {
				pstmt.setLong(3, subjectId);
			}
			ResultSet rs = pstmt.executeQuery();
			boolean allocated = rs.next() && rs.getInt(1) > 0;
			rs.close();
			pstmt.close();
			return allocated;
		} catch (Exception e) {
			log.error("Faculty allocation verification failed closed for " + email, e);
			return false;
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	@Override
	protected void buildWhereParameters(FacultyAllocationBean bean, StringBuilder whereSql, List<Object> params) {
		if (bean == null) {
			return;
		}
		if (bean.getId() > 0) {
			whereSql.append(" AND ID = ?");
			params.add(bean.getId());
		}
		if (bean.getFacultyId() > 0) {
			whereSql.append(" AND FACULTY_ID = ?");
			params.add(bean.getFacultyId());
		}
		if (DataValidator.isNotNull(bean.getFacultyEmail())) {
			whereSql.append(" AND LOWER(FACULTY_EMAIL) = ?");
			params.add(bean.getFacultyEmail().trim().toLowerCase());
		}
		if (bean.getCourseId() > 0) {
			whereSql.append(" AND COURSE_ID = ?");
			params.add(bean.getCourseId());
		}
		if (bean.getSubjectId() > 0) {
			whereSql.append(" AND SUBJECT_ID = ?");
			params.add(bean.getSubjectId());
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
			whereSql.append(" AND FACULTY_ID IN (SELECT ID FROM ST_FACULTY WHERE COLLEGE_ID = ?)");
			params.add(bean.getScopedCollegeId());
		}
	}

	@Override
	public String getWhereClause(FacultyAllocationBean bean) {
		StringBuilder sql = new StringBuilder();
		if (bean != null && bean.getId() > 0) {
			sql.append(" AND ID = ").append(bean.getId());
		}
		return sql.toString();
	}

	@Override
	public String getTable() {
		return "ST_FACULTY_ALLOCATION";
	}

	@Override
	public FacultyAllocationBean getBean() {
		return new FacultyAllocationBean();
	}
}
