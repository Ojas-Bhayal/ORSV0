package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CollegeBean;
import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.StudentBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.JDBCDataSource;

/**
 * JDBC Implementation of Student Model (Phase 4 Transactional Enrollment Sync).
 * Performs ST_STUDENT and ST_ENROLLMENT updates in a single atomic transaction
 * and reconciles obsolete enrollments when a student's course changes.
 */
public class StudentModel extends BaseModel<StudentBean> {

	private static Logger log = Logger.getLogger(StudentModel.class);

	public StudentBean findByEmailId(String email) throws ApplicationException {
		return findByUniqueColumn("EMAIL", email);
	}

	private void resolveNames(StudentBean bean) throws ApplicationException {
		if (bean.getCollegeId() != null && bean.getCollegeId() > 0) {
			CollegeModel cModel = new CollegeModel();
			CollegeBean collegeBean = cModel.findByPK(bean.getCollegeId());
			if (collegeBean != null) {
				bean.setCollegeName(collegeBean.getName());
			}
		}
		if (bean.getCourseId() > 0) {
			CourseModel courseModel = new CourseModel();
			CourseBean courseBean = courseModel.findByPK(bean.getCourseId());
			if (courseBean != null) {
				bean.setCourseName(courseBean.getName());
			}
		}
	}

	@Override
	public long add(StudentBean bean) throws ApplicationException, DuplicateRecordException {
		String colums = "ID,COLLEGE_ID, COLLEGE_NAME,FIRST_NAME,LAST_NAME,DATE_OF_BIRTH, MOBILE_NO,EMAIL,COURSE_ID,COURSE_NAME";
		String values = "?,?,?,?,?,?,?,?,?,?";

		StringBuffer sql = new StringBuffer("INSERT INTO " + getTable());
		sql.append("(CREATED_DATETIME,MODIFIED_DATETIME,CREATED_BY,MODIFIED_BY, " + colums + ")");
		sql.append(" VALUES(NOW(),NOW(),?,?, " + values + " )");

		checkDuplicate(bean);
		resolveNames(bean);

		Connection conn = null;
		int pk = 0;

		try {
			pk = nextPK();
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);

			PreparedStatement pstmt = conn.prepareStatement(sql.toString());
			pstmt.setString(1, bean.getCreatedBy() != null ? bean.getCreatedBy() : getDefaultUser());
			pstmt.setString(2, bean.getModifiedBy() != null ? bean.getModifiedBy() : getDefaultUser());
			pstmt.setInt(3, pk);
			pstmt.setLong(4, bean.getCollegeId());
			pstmt.setString(5, bean.getCollegeName());
			pstmt.setString(6, bean.getFirstName());
			pstmt.setString(7, bean.getLastName());
			pstmt.setDate(8, bean.getDob() != null ? new java.sql.Date(bean.getDob().getTime()) : null);
			pstmt.setString(9, bean.getMobileNo());
			pstmt.setString(10, bean.getEmail());
			pstmt.setLong(11, bean.getCourseId());
			pstmt.setString(12, bean.getCourseName());

			pstmt.executeUpdate();
			pstmt.close();

			// Phase 4 Tasks 3 & 5: Transactional enrollment sync on same Connection
			if (bean.getCourseId() > 0) {
				EnrollmentModel enrollmentModel = new EnrollmentModel();
				enrollmentModel.reconcileStudentEnrollment(conn, pk, bean, 0L);
			}

			conn.commit();
		} catch (Exception e) {
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in adding Student and Enrollment: " + e.getMessage());
		} finally {
			JDBCDataSource.closeConnection(conn);
		}

		return pk;
	}

	@Override
	public void update(StudentBean bean) throws ApplicationException, DuplicateRecordException {
		String sql = "UPDATE ST_STUDENT SET COLLEGE_ID=?,COLLEGE_NAME=?,FIRST_NAME=?,LAST_NAME=?,DATE_OF_BIRTH=?,MOBILE_NO=?,EMAIL=?,COURSE_ID=?,COURSE_NAME=? WHERE ID=?";

		StudentBean beanExist = findByEmailId(bean.getEmail());
		if (beanExist != null && beanExist.getId() != bean.getId()) {
			throw new DuplicateRecordException("Email Id is already exist");
		}

		StudentBean priorRecord = findByPK(bean.getId());
		long previousCourseId = (priorRecord != null) ? priorRecord.getCourseId() : 0L;

		resolveNames(bean);

		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, bean.getCollegeId());
			pstmt.setString(2, bean.getCollegeName());
			pstmt.setString(3, bean.getFirstName());
			pstmt.setString(4, bean.getLastName());
			pstmt.setDate(5, bean.getDob() != null ? new java.sql.Date(bean.getDob().getTime()) : null);
			pstmt.setString(6, bean.getMobileNo());
			pstmt.setString(7, bean.getEmail());
			pstmt.setLong(8, bean.getCourseId());
			pstmt.setString(9, bean.getCourseName());
			pstmt.setLong(10, bean.getId());
			pstmt.executeUpdate();
			pstmt.close();

			updatedTimestamp(bean.getId(), conn);

			// Phase 4 Tasks 3, 4, 5: Transactional enrollment reconciliation on same Connection
			if (bean.getCourseId() > 0) {
				EnrollmentModel enrollmentModel = new EnrollmentModel();
				enrollmentModel.reconcileStudentEnrollment(conn, bean.getId(), bean, previousCourseId);
			}

			conn.commit();
		} catch (Exception e) {
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in updating Student and Enrollment: " + e.getMessage());
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	@Override
	protected void buildWhereParameters(StudentBean bean, StringBuilder sql, List<Object> params) {
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
			if (bean.getMobileNo() != null && !bean.getMobileNo().isEmpty()) {
				sql.append(" AND MOBILE_NO LIKE ?");
				params.add(bean.getMobileNo() + "%");
			}
			if (bean.getEmail() != null && !bean.getEmail().isEmpty()) {
				sql.append(" AND EMAIL LIKE ?");
				params.add(bean.getEmail() + "%");
			}
			if (bean.getCollegeId() != null && bean.getCollegeId() > 0) {
				sql.append(" AND COLLEGE_ID = ?");
				params.add(bean.getCollegeId());
			}
			if (bean.getCourseId() > 0) {
				sql.append(" AND ID IN (SELECT STUDENT_ID FROM ST_ENROLLMENT WHERE COURSE_ID = ? AND STATUS = 'ACTIVE')");
				params.add(bean.getCourseId());
			}
			if (bean.getScopedCollegeId() != null && bean.getScopedCollegeId() > 0) {
				sql.append(" AND COLLEGE_ID = ?");
				params.add(bean.getScopedCollegeId());
			}
			if (bean.getScopedCourseIds() != null && !bean.getScopedCourseIds().isEmpty()) {
				sql.append(" AND ID IN (SELECT STUDENT_ID FROM ST_ENROLLMENT WHERE STATUS = 'ACTIVE' AND COURSE_ID IN (");
				int idx = 0;
				for (Long cid : bean.getScopedCourseIds()) {
					if (idx++ > 0) {
						sql.append(",");
					}
					sql.append("?");
					params.add(cid);
				}
				sql.append("))");
			}
		}
	}

	@Override
	public String getWhereClause(StudentBean bean) {
		StringBuilder sql = new StringBuilder();
		if (bean != null) {
			if (bean.getId() > 0) {
				sql.append(" AND ID = ").append(bean.getId());
			}
			if (bean.getCollegeId() != null && bean.getCollegeId() > 0) {
				sql.append(" AND COLLEGE_ID = ").append(bean.getCollegeId());
			}
			if (bean.getCourseId() > 0) {
				sql.append(" AND COURSE_ID = ").append(bean.getCourseId());
			}
		}
		return sql.toString();
	}

	@Override
	public void checkDuplicate(StudentBean bean) {
		StudentBean duplicateBean = this.findByEmailId(bean.getEmail());
		if (duplicateBean != null && duplicateBean.getId() != bean.getId()) {
			throw new DuplicateRecordException("Email already exists");
		}
		if (bean.getId() == 0 && duplicateBean != null) {
			throw new DuplicateRecordException("Email already exists");
		}
	}

	@Override
	public String getTable() {
		return "ST_STUDENT";
	}

	@Override
	public StudentBean getBean() {
		return new StudentBean();
	}
}
