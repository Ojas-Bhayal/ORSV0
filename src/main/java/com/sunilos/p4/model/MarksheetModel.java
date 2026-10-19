package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.EnrollmentBean;
import com.sunilos.p4.bean.MarksheetBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.JDBCDataSource;
import com.sunilos.p4.util.PropertyReader;

/**
 * Authoritative Course-Wise and Subject-Wise MarksheetModel (Phases 3, 4, 5, 6).
 * - Uses STUDENT_ID as authoritative identity and resolves/verifies ROLL_NO from
 *   the student's active ST_ENROLLMENT record rather than trusting unverified input.
 * - Verifies student existence, course existence, subject-to-course membership,
 *   active enrollment, faculty subject/course allocation, and 0..100 integer range.
 * - Filters merit rankings and class averages by BOTH courseId and subjectId
 *   across actively enrolled students.
 * - Applies SQL-level row scoping (scopedCourseIds, scopedSubjectIds, scopedStudentId).
 */
public class MarksheetModel extends BaseModel<MarksheetBean> {

	Logger log = Logger.getLogger(MarksheetModel.class);

	private void validateAndResolveRelationships(MarksheetBean bean) throws ApplicationException {
		if (bean.getStudentId() == null || bean.getStudentId() <= 0) {
			throw new ApplicationException(PropertyReader.getValue("error.require", "Student Name"));
		}
		if (bean.getCourseId() <= 0) {
			throw new ApplicationException(PropertyReader.getValue("error.require", "Course"));
		}
		if (bean.getSubjectId() <= 0) {
			throw new ApplicationException(PropertyReader.getValue("error.require", "Subject"));
		}
		if (bean.getMarks() == null || bean.getMarks() < 0 || bean.getMarks() > 100) {
			throw new ApplicationException(PropertyReader.getValue("error.marks.max"));
		}

		StudentModel sModel = new StudentModel();
		StudentBean student = sModel.findByPK(bean.getStudentId());
		if (student == null) {
			throw new ApplicationException("Student record not found for ID: " + bean.getStudentId());
		}
		bean.setName(student.getFirstName() + " " + student.getLastName());

		CourseModel cModel = new CourseModel();
		CourseBean course = cModel.findByPK(bean.getCourseId());
		if (course == null) {
			throw new ApplicationException("Course record not found for ID: " + bean.getCourseId());
		}
		bean.setCourseName(course.getName());

		SubjectModel subModel = new SubjectModel();
		SubjectBean subject = subModel.findByPK(bean.getSubjectId());
		if (subject == null) {
			throw new ApplicationException("Subject record not found for ID: " + bean.getSubjectId());
		}
		if (subject.getCourseId() != bean.getCourseId()) {
			throw new ApplicationException(PropertyReader.getValue("error.subject.course.mismatch"));
		}
		bean.setSubjectName(subject.getName());

		// Phase 4 Task 4 & 5: Verify active enrollment and resolve authoritative ROLL_NO from ST_ENROLLMENT
		EnrollmentModel enrollmentModel = new EnrollmentModel();
		if (!enrollmentModel.isStudentEnrolledInCourse(bean.getStudentId(), bean.getCourseId())) {
			throw new ApplicationException(PropertyReader.getValue("error.enrollment.required"));
		}

		EnrollmentBean activeEnrollment = enrollmentModel.findByStudentAndCourse(bean.getStudentId(),
				bean.getCourseId());
		if (activeEnrollment != null && DataValidator.isNotNull(activeEnrollment.getRollNo())) {
			String authoritativeRoll = activeEnrollment.getRollNo().trim();
			if (DataValidator.isNotNull(bean.getRollNo())
					&& !authoritativeRoll.equalsIgnoreCase(bean.getRollNo().trim())) {
				throw new ApplicationException(PropertyReader.getValue("error.rollno.student.mismatch"));
			}
			bean.setRollNo(authoritativeRoll);
		} else if (DataValidator.isNull(bean.getRollNo()) || !enrollmentModel
				.verifyRollNoBelongsToStudent(bean.getStudentId(), bean.getCourseId(), bean.getRollNo())) {
			throw new ApplicationException(PropertyReader.getValue("error.rollno.student.mismatch"));
		}

		// Phase 4 Task 5: If an actorFacultyEmail is set on the bean, verify faculty allocation in model layer too
		if (DataValidator.isNotNull(bean.getActorFacultyEmail())) {
			FacultyAllocationModel allocModel = new FacultyAllocationModel();
			if (!allocModel.isFacultyAllocatedByEmail(bean.getActorFacultyEmail(), bean.getCourseId(),
					bean.getSubjectId())) {
				throw new ApplicationException(PropertyReader.getValue("error.scope.denied"));
			}
		}
	}

	public MarksheetBean findByStudentCourseAndSubject(long studentId, long courseId, long subjectId)
			throws ApplicationException {
		String sql = "SELECT * FROM ST_MARKSHEET WHERE STUDENT_ID=? AND COURSE_ID=? AND SUBJECT_ID=?";
		MarksheetBean bean = null;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, studentId);
			pstmt.setLong(2, courseId);
			pstmt.setLong(3, subjectId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				bean = populateBean(rs);
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception in findByStudentCourseAndSubject", e);
			throw new ApplicationException("Exception in getting marksheet by studentId, course, and subject");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return bean;
	}

	public MarksheetBean findByRollNoCourseAndSubject(String rollNo, long courseId, long subjectId)
			throws ApplicationException {
		String sql = "SELECT * FROM ST_MARKSHEET WHERE ROLL_NO=? AND COURSE_ID=? AND SUBJECT_ID=?";
		MarksheetBean bean = null;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, rollNo);
			pstmt.setLong(2, courseId);
			pstmt.setLong(3, subjectId);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				bean = populateBean(rs);
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception in findByRollNoCourseAndSubject", e);
			throw new ApplicationException("Exception in getting marksheet by rollNo, course, and subject");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return bean;
	}

	@Override
	public long add(MarksheetBean bean) throws ApplicationException, DuplicateRecordException {
		log.debug("Model add Started");
		Connection conn = null;

		validateAndResolveRelationships(bean);

		MarksheetBean dupByStudent = findByStudentCourseAndSubject(bean.getStudentId(), bean.getCourseId(),
				bean.getSubjectId());
		if (dupByStudent != null) {
			throw new DuplicateRecordException("Marks for Student in Course " + bean.getCourseName() + " ("
					+ bean.getSubjectName() + ") already exist");
		}
		MarksheetBean dupByRoll = findByRollNoCourseAndSubject(bean.getRollNo(), bean.getCourseId(),
				bean.getSubjectId());
		if (dupByRoll != null) {
			throw new DuplicateRecordException("Marks for Roll Number " + bean.getRollNo() + " in Course "
					+ bean.getCourseName() + " (" + bean.getSubjectName() + ") already exist");
		}

		int score = bean.getMarks();
		int pk = 0;
		try {
			conn = JDBCDataSource.getConnection();
			pk = nextPK();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement(
					"INSERT INTO ST_MARKSHEET (ID,ROLL_NO,STUDENT_ID,NAME,COURSE_ID,COURSE_NAME,SUBJECT_ID,SUBJECT_NAME,MARKS,CREATED_BY,MODIFIED_BY,CREATED_DATETIME,MODIFIED_DATETIME) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)");
			pstmt.setInt(1, pk);
			pstmt.setString(2, bean.getRollNo().trim());
			pstmt.setLong(3, bean.getStudentId());
			pstmt.setString(4, bean.getName());
			pstmt.setLong(5, bean.getCourseId());
			pstmt.setString(6, bean.getCourseName());
			pstmt.setLong(7, bean.getSubjectId());
			pstmt.setString(8, bean.getSubjectName());
			pstmt.setInt(9, score);
			pstmt.setString(10, bean.getCreatedBy());
			pstmt.setString(11, bean.getModifiedBy());
			pstmt.setTimestamp(12, bean.getCreatedDatetime());
			pstmt.setTimestamp(13, bean.getModifiedDatetime());
			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();
		} catch (Exception e) {
			log.error(e);
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in add marksheet");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model add End");
		return pk;
	}

	public MarksheetBean findByRollNo(String rollNo) throws ApplicationException {
		MarksheetBean bean = findByUniqueColumn("ROLL_NO", rollNo);
		if (bean != null) {
			List<MarksheetBean> courseMarksheets = findCourseWiseMarksByRollNo(rollNo);
			if (!courseMarksheets.isEmpty()) {
				MarksheetBean firstCourse = courseMarksheets.get(0);
				bean.setSubjectMarks(firstCourse.getSubjectMarks());
				bean.setSubjectClassAverages(firstCourse.getSubjectClassAverages());
				bean.setClassAverage(firstCourse.getClassAverage());
			}
		}
		return bean;
	}

	public List<MarksheetBean> findCourseWiseMarksByRollNo(String rollNo) throws ApplicationException {
		if (rollNo == null || rollNo.trim().isEmpty()) {
			return new ArrayList<>();
		}
		return findCourseWiseMarks(null, 0L, rollNo.trim());
	}

	/**
	 * Loads the course-wise marks for an authoritative student/course relationship.
	 * This avoids depending on the same roll number being reused across every active
	 * enrollment, and is used by the authenticated student's own-results workflow.
	 */
	public List<MarksheetBean> findCourseWiseMarksByStudentIdAndCourseId(long studentId, long courseId)
			throws ApplicationException {
		if (studentId <= 0 || courseId <= 0) {
			return new ArrayList<>();
		}
		return findCourseWiseMarks(Long.valueOf(studentId), courseId, null);
	}

	private List<MarksheetBean> findCourseWiseMarks(Long studentId, long courseId, String rollNo)
			throws ApplicationException {
		List<MarksheetBean> result = new ArrayList<>();
		if ((rollNo == null || rollNo.trim().isEmpty()) && (studentId == null || studentId <= 0 || courseId <= 0)) {
			return result;
		}

		StringBuilder sql = new StringBuilder(
				"SELECT m.*, c.NAME AS REAL_COURSE_NAME, s.NAME AS REAL_SUBJECT_NAME "
						+ "FROM ST_MARKSHEET m "
						+ "INNER JOIN ST_COURSE c ON m.COURSE_ID = c.ID "
						+ "INNER JOIN ST_SUBJECT s ON m.SUBJECT_ID = s.ID AND s.COURSE_ID = c.ID "
						+ "WHERE EXISTS (SELECT 1 FROM ST_ENROLLMENT e "
						+ "WHERE e.STUDENT_ID = m.STUDENT_ID AND e.COURSE_ID = m.COURSE_ID AND e.STATUS = 'ACTIVE') ");
		List<Object> params = new ArrayList<>();
		if (rollNo != null && !rollNo.trim().isEmpty()) {
			sql.append(" AND m.ROLL_NO = ?");
			params.add(rollNo.trim());
		}
		if (studentId != null && studentId > 0) {
			sql.append(" AND m.STUDENT_ID = ?");
			params.add(studentId);
		}
		if (courseId > 0) {
			sql.append(" AND m.COURSE_ID = ?");
			params.add(courseId);
		}
		sql.append(" ORDER BY m.COURSE_ID ASC, m.SUBJECT_ID ASC, m.ID ASC");

		Map<Long, MarksheetBean> courseGroupMap = new LinkedHashMap<>();
		Map<Long, Map<Long, String>> courseSubjectIdToNameMap = new LinkedHashMap<>();
		Long matchedStudentId = null;
		boolean ambiguousRollNumber = false;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
				for (int i = 0; i < params.size(); i++) {
					pstmt.setObject(i + 1, params.get(i));
				}
				try (ResultSet rs = pstmt.executeQuery()) {
					while (rs.next()) {
						MarksheetBean row = populateBean(rs);
						if (rollNo != null && !rollNo.trim().isEmpty()) {
							Long rowStudentId = row.getStudentId();
							if (rowStudentId == null || rowStudentId <= 0) {
								ambiguousRollNumber = true;
							} else if (matchedStudentId == null) {
								matchedStudentId = rowStudentId;
							} else if (!matchedStudentId.equals(rowStudentId)) {
								ambiguousRollNumber = true;
							}
						}
						String realCourseName = rs.getString("REAL_COURSE_NAME");
						if (realCourseName != null && !realCourseName.isEmpty()) {
							row.setCourseName(realCourseName);
						}
						String realSubjectName = rs.getString("REAL_SUBJECT_NAME");
						if (realSubjectName != null && !realSubjectName.isEmpty()) {
							row.setSubjectName(realSubjectName);
						}

						long cId = row.getCourseId();
						long sId = row.getSubjectId();
						MarksheetBean courseBean = courseGroupMap.get(cId);
						if (courseBean == null) {
							courseBean = row;
							courseBean.setSubjectMarks(new LinkedHashMap<>());
							courseBean.setSubjectClassAverages(new LinkedHashMap<>());
							courseGroupMap.put(cId, courseBean);
							courseSubjectIdToNameMap.put(cId, new LinkedHashMap<>());
						}
						courseBean.addSubjectMark(row.getSubjectName(), row.getMarks());
						if (sId > 0 && row.getSubjectName() != null) {
							courseSubjectIdToNameMap.get(cId).put(sId, row.getSubjectName());
						}
					}
				}
			}
		} catch (Exception e) {
			log.error("Database Exception in course-wise marksheet query", e);
			throw new ApplicationException("Exception in getting course-wise marksheet results");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}

		if (ambiguousRollNumber) {
			throw new ApplicationException("Roll number matches multiple student records. Refine the search using the correct student record.");
		}

		for (Map.Entry<Long, MarksheetBean> entry : courseGroupMap.entrySet()) {
			long cId = entry.getKey();
			MarksheetBean cBean = entry.getValue();
			Map<Long, String> subjMap = courseSubjectIdToNameMap.get(cId);
			double sumSubjAvgs = 0.0;
			int subjCount = 0;
			if (subjMap != null && !subjMap.isEmpty()) {
				for (Map.Entry<Long, String> se : subjMap.entrySet()) {
					double subjAvg = getClassAverageForCourseAndSubject(cId, se.getKey());
					cBean.addSubjectClassAverage(se.getValue(), subjAvg);
					sumSubjAvgs += subjAvg;
					subjCount++;
				}
			}
			double courseAvg = (subjCount > 0) ? (sumSubjAvgs / subjCount) : getClassAverageForCourse(cId);
			cBean.setClassAverage(courseAvg);
			result.add(cBean);
		}
		return result;
	}

	/**
	 * Phase 6 Task 2: Calculates the class average using ONLY marks for the
	 * selected courseId and subjectId across actively enrolled students.
	 */
	public double getClassAverageForCourseAndSubject(long courseId, long subjectId) throws ApplicationException {
		if (courseId <= 0) {
			throw new ApplicationException(PropertyReader.getValue("error.require", "Course"));
		}
		if (subjectId <= 0) {
			throw new ApplicationException(PropertyReader.getValue("error.require", "Subject"));
		}
		CourseModel cModel = new CourseModel();
		CourseBean course = cModel.findByPK(courseId);
		if (course == null) {
			throw new ApplicationException("Course record not found for ID: " + courseId);
		}
		SubjectModel subModel = new SubjectModel();
		SubjectBean subject = subModel.findByPK(subjectId);
		if (subject == null || subject.getCourseId() != courseId) {
			throw new ApplicationException(PropertyReader.getValue("error.subject.course.mismatch"));
		}

		double avg = 0.0;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			String sql = "SELECT AVG(m.MARKS) FROM ST_MARKSHEET m "
					+ "INNER JOIN ST_SUBJECT s ON s.ID = m.SUBJECT_ID AND s.COURSE_ID = m.COURSE_ID "
					+ "WHERE m.COURSE_ID = ? AND m.SUBJECT_ID = ? "
					+ "AND EXISTS (SELECT 1 FROM ST_ENROLLMENT e WHERE e.STUDENT_ID = m.STUDENT_ID "
					+ "AND e.COURSE_ID = m.COURSE_ID AND UPPER(TRIM(e.STATUS)) = 'ACTIVE')";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setLong(1, courseId);
				pstmt.setLong(2, subjectId);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						double value = rs.getDouble(1);
						avg = rs.wasNull() ? Double.NaN : value;
					}
				}
			}
		} catch (Exception e) {
			log.error("Database Exception in getClassAverageForCourseAndSubject", e);
			throw new ApplicationException("Exception in computing class average for course and subject");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return avg;
	}

	/**
	 * Computes a subject-specific class average inside one authorized college.
	 * A missing set of marks returns NaN (unavailable), not a misleading zero.
	 */
	public double getClassAverageForCourseSubjectAndCollege(long courseId, long subjectId, long collegeId)
			throws ApplicationException {
		if (courseId <= 0 || subjectId <= 0 || collegeId <= 0) {
			throw new ApplicationException("A valid course, subject, and college are required for scoped subject averages");
		}
		CourseModel courseModel = new CourseModel();
		if (courseModel.findByPK(courseId) == null) {
			throw new ApplicationException("Course record not found for ID: " + courseId);
		}
		SubjectModel subjectModel = new SubjectModel();
		SubjectBean subject = subjectModel.findByPK(subjectId);
		if (subject == null || subject.getCourseId() != courseId) {
			throw new ApplicationException(PropertyReader.getValue("error.subject.course.mismatch"));
		}

		String sql = "SELECT AVG(m.MARKS) FROM ST_MARKSHEET m "
				+ "INNER JOIN ST_SUBJECT s ON s.ID=m.SUBJECT_ID AND s.COURSE_ID=m.COURSE_ID "
				+ "INNER JOIN ST_STUDENT st ON st.ID=m.STUDENT_ID "
				+ "WHERE m.COURSE_ID=? AND m.SUBJECT_ID=? AND st.COLLEGE_ID=? "
				+ "AND EXISTS (SELECT 1 FROM ST_ENROLLMENT e WHERE e.STUDENT_ID=m.STUDENT_ID "
				+ "AND e.COURSE_ID=m.COURSE_ID AND UPPER(TRIM(e.STATUS))='ACTIVE')";
		try (Connection conn = JDBCDataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, courseId);
			stmt.setLong(2, subjectId);
			stmt.setLong(3, collegeId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					double value = rs.getDouble(1);
					return rs.wasNull() ? Double.NaN : value;
				}
				return Double.NaN;
			}
		} catch (Exception e) {
			log.error("Database Exception in college-scoped subject average", e);
			throw new ApplicationException("Exception in computing college-scoped class average for subject");
		}
	}

	/** Computes a course average using only active enrollments from one college. */
	public double getClassAverageForCourseAndCollege(long courseId, long collegeId) throws ApplicationException {
		if (courseId <= 0 || collegeId <= 0) {
			throw new ApplicationException("A valid course and college are required for scoped class averages");
		}
		CourseModel courseModel = new CourseModel();
		if (courseModel.findByPK(courseId) == null) {
			throw new ApplicationException("Course record not found for ID: " + courseId);
		}
		String sql = "SELECT AVG(m.MARKS) FROM ST_MARKSHEET m "
				+ "INNER JOIN ST_SUBJECT s ON s.ID=m.SUBJECT_ID AND s.COURSE_ID=m.COURSE_ID "
				+ "INNER JOIN ST_STUDENT st ON st.ID=m.STUDENT_ID "
				+ "WHERE m.COURSE_ID=? AND st.COLLEGE_ID=? "
				+ "AND EXISTS (SELECT 1 FROM ST_ENROLLMENT e WHERE e.STUDENT_ID=m.STUDENT_ID "
				+ "AND e.COURSE_ID=m.COURSE_ID AND UPPER(TRIM(e.STATUS))='ACTIVE')";
		try (Connection conn = JDBCDataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, courseId);
			stmt.setLong(2, collegeId);
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					double value = rs.getDouble(1);
					return rs.wasNull() ? Double.NaN : value;
				}
				return Double.NaN;
			}
		} catch (Exception e) {
			log.error("Database Exception in college-scoped course average", e);
			throw new ApplicationException("Exception in computing college-scoped class average");
		}
	}

	public double getClassAverageForCourse(long courseId) throws ApplicationException {
		if (courseId <= 0) {
			throw new ApplicationException(PropertyReader.getValue("error.require", "Course"));
		}
		CourseModel cModel = new CourseModel();
		CourseBean course = cModel.findByPK(courseId);
		if (course == null) {
			throw new ApplicationException("Course record not found for ID: " + courseId);
		}

		double avg = 0.0;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			String sql = "SELECT AVG(m.MARKS) FROM ST_MARKSHEET m "
					+ "INNER JOIN ST_SUBJECT s ON s.ID = m.SUBJECT_ID AND s.COURSE_ID = m.COURSE_ID "
					+ "WHERE m.COURSE_ID = ? "
					+ "AND EXISTS (SELECT 1 FROM ST_ENROLLMENT e WHERE e.STUDENT_ID = m.STUDENT_ID "
					+ "AND e.COURSE_ID = m.COURSE_ID AND UPPER(TRIM(e.STATUS)) = 'ACTIVE')";
			try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
				pstmt.setLong(1, courseId);
				try (ResultSet rs = pstmt.executeQuery()) {
					if (rs.next()) {
						double value = rs.getDouble(1);
						avg = rs.wasNull() ? Double.NaN : value;
					}
				}
			}
		} catch (Exception e) {
			log.error("Database Exception in getClassAverageForCourse", e);
			throw new ApplicationException("Exception in computing class average for course");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return avg;
	}

	@Override
	public void update(MarksheetBean bean) throws ApplicationException, DuplicateRecordException {
		log.debug("Model update Started");

		Connection conn = null;
		validateAndResolveRelationships(bean);

		MarksheetBean dupByStudent = findByStudentCourseAndSubject(bean.getStudentId(), bean.getCourseId(),
				bean.getSubjectId());
		if (dupByStudent != null && dupByStudent.getId() != bean.getId()) {
			throw new DuplicateRecordException("Student marks already exist for this Course and Subject");
		}
		MarksheetBean dupByRoll = findByRollNoCourseAndSubject(bean.getRollNo(), bean.getCourseId(),
				bean.getSubjectId());
		if (dupByRoll != null && dupByRoll.getId() != bean.getId()) {
			throw new DuplicateRecordException("Roll No and Subject marks already exist for this Course");
		}

		int score = bean.getMarks();

		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement(
					"UPDATE ST_MARKSHEET SET ROLL_NO=?,STUDENT_ID=?,NAME=?,COURSE_ID=?,COURSE_NAME=?,SUBJECT_ID=?,SUBJECT_NAME=?,MARKS=?,CREATED_BY=?,MODIFIED_BY=?,CREATED_DATETIME=?,MODIFIED_DATETIME=? WHERE ID=?");
			pstmt.setString(1, bean.getRollNo().trim());
			pstmt.setLong(2, bean.getStudentId());
			pstmt.setString(3, bean.getName());
			pstmt.setLong(4, bean.getCourseId());
			pstmt.setString(5, bean.getCourseName());
			pstmt.setLong(6, bean.getSubjectId());
			pstmt.setString(7, bean.getSubjectName());
			pstmt.setInt(8, score);
			pstmt.setString(9, bean.getCreatedBy());
			pstmt.setString(10, bean.getModifiedBy());
			pstmt.setTimestamp(11, bean.getCreatedDatetime());
			pstmt.setTimestamp(12, bean.getModifiedDatetime());
			pstmt.setLong(13, bean.getId());
			pstmt.executeUpdate();
			updatedTimestamp(bean.getId(), conn);
			conn.commit();
			pstmt.close();
		} catch (Exception e) {
			log.error(e);
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in updating Marksheet ");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}

		log.debug("Model update End");
	}

	public List<MarksheetBean> getMeritList(int pageNo, int pageSize) throws ApplicationException {
		return getMeritListByCourseAndSubject(0L, 0L, pageNo, pageSize);
	}

	public List<MarksheetBean> getMeritListByCourse(long courseId, int pageNo, int pageSize)
			throws ApplicationException {
		return getMeritListByCourseAndSubject(courseId, 0L, pageNo, pageSize);
	}

	public List<MarksheetBean> getMeritListByCourseAndSubject(long courseId, long subjectId, int pageNo, int pageSize)
			throws ApplicationException {
		log.debug("Model getMeritListByCourseAndSubject Started");
		if (courseId > 0 && subjectId > 0) {
			SubjectModel subModel = new SubjectModel();
			SubjectBean subject = subModel.findByPK(subjectId);
			if (subject == null || subject.getCourseId() != courseId) {
				throw new ApplicationException(PropertyReader.getValue("error.subject.course.mismatch"));
			}
		}
		ArrayList<MarksheetBean> list = new ArrayList<>();
		StringBuilder sql = new StringBuilder(
				"SELECT m.ID, m.ROLL_NO, m.STUDENT_ID, m.NAME, m.COURSE_ID, m.COURSE_NAME, m.SUBJECT_ID, m.SUBJECT_NAME, m.MARKS, m.CREATED_BY, m.MODIFIED_BY, m.CREATED_DATETIME, m.MODIFIED_DATETIME "
						+ "FROM ST_MARKSHEET m "
						+ "INNER JOIN ST_COURSE c ON m.COURSE_ID = c.ID "
						+ "INNER JOIN ST_SUBJECT s ON m.SUBJECT_ID = s.ID AND s.COURSE_ID = m.COURSE_ID "
						+ "INNER JOIN ST_ENROLLMENT e ON m.STUDENT_ID = e.STUDENT_ID AND m.COURSE_ID = e.COURSE_ID "
						+ "WHERE e.STATUS = 'ACTIVE'");
		List<Object> params = new ArrayList<>();
		if (courseId > 0) {
			sql.append(" AND m.COURSE_ID = ?");
			params.add(courseId);
		}
		if (subjectId > 0) {
			sql.append(" AND m.SUBJECT_ID = ?");
			params.add(subjectId);
		}
		sql.append(" ORDER BY m.MARKS DESC, m.ID ASC");

		if (pageSize > 0) {
			int offset = (pageNo - 1) * pageSize;
			sql.append(" LIMIT ?, ?");
			params.add(offset);
			params.add(pageSize);
		}

		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql.toString());
			for (int i = 0; i < params.size(); i++) {
				pstmt.setObject(i + 1, params.get(i));
			}
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				MarksheetBean bean = populateBean(rs);
				list.add(bean);
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error(e);
			throw new ApplicationException("Exception in getting merit list of Marksheet");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model getMeritListByCourseAndSubject End");
		return list;
	}

	@Override
	protected void buildWhereParameters(MarksheetBean bean, StringBuilder sql, List<Object> params) {
		if (bean != null) {
			if (bean.getId() > 0) {
				sql.append(" AND ID = ?");
				params.add(bean.getId());
			}
			if (bean.getRollNo() != null && !bean.getRollNo().isEmpty()) {
				sql.append(" AND ROLL_NO LIKE ?");
				params.add(bean.getRollNo() + "%");
			}
			if (bean.getName() != null && !bean.getName().isEmpty()) {
				sql.append(" AND NAME LIKE ?");
				params.add(bean.getName() + "%");
			}
			if (bean.getStudentId() != null && bean.getStudentId() > 0) {
				sql.append(" AND STUDENT_ID = ?");
				params.add(bean.getStudentId());
			}
			if (bean.getScopedStudentId() != null && bean.getScopedStudentId() > 0) {
				sql.append(" AND STUDENT_ID = ?");
				params.add(bean.getScopedStudentId());
			}
			if (bean.getScopedCollegeId() != null && bean.getScopedCollegeId() > 0) {
				sql.append(" AND STUDENT_ID IN (SELECT ID FROM ST_STUDENT WHERE COLLEGE_ID = ?)");
				params.add(bean.getScopedCollegeId());
			}
			if (bean.getCourseId() > 0) {
				sql.append(" AND COURSE_ID = ?");
				params.add(bean.getCourseId());
			}
			if (bean.getScopedCourseIds() != null && !bean.getScopedCourseIds().isEmpty()) {
				sql.append(" AND COURSE_ID IN (");
				int idx = 0;
				for (Long cid : bean.getScopedCourseIds()) {
					if (idx++ > 0) {
						sql.append(",");
					}
					sql.append("?");
					params.add(cid);
				}
				sql.append(")");
			}
			if (bean.getSubjectId() > 0) {
				sql.append(" AND SUBJECT_ID = ?");
				params.add(bean.getSubjectId());
			}
			if (bean.getScopedSubjectIds() != null && !bean.getScopedSubjectIds().isEmpty()) {
				sql.append(" AND SUBJECT_ID IN (");
				int idx = 0;
				for (Long sid : bean.getScopedSubjectIds()) {
					if (idx++ > 0) {
						sql.append(",");
					}
					sql.append("?");
					params.add(sid);
				}
				sql.append(")");
			}
			if (bean.getMarks() != null && bean.getMarks() >= 0) {
				sql.append(" AND MARKS = ?");
				params.add(bean.getMarks());
			}
		}
	}

	@Override
	public String getTable() {
		return "ST_MARKSHEET";
	}

	@Override
	public MarksheetBean getBean() {
		return new MarksheetBean();
	}

	@Override
	public String getWhereClause(MarksheetBean bean) {
		StringBuilder sql = new StringBuilder();
		if (bean != null) {
			if (bean.getId() > 0) {
				sql.append(" AND ID = ").append(bean.getId());
			}
			if (bean.getStudentId() != null && bean.getStudentId() > 0) {
				sql.append(" AND STUDENT_ID = ").append(bean.getStudentId());
			}
			if (bean.getCourseId() > 0) {
				sql.append(" AND COURSE_ID = ").append(bean.getCourseId());
			}
			if (bean.getSubjectId() > 0) {
				sql.append(" AND SUBJECT_ID = ").append(bean.getSubjectId());
			}
		}
		return sql.toString();
	}
}
