package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CollegeBean;
import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.FacultyBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.JDBCDataSource;

/**
 * JDBC Implementation of FacultyModel (Phase 4 Transactional Allocation Sync).
 * Performs ST_FACULTY and ST_FACULTY_ALLOCATION updates in a single atomic
 * transaction and reconciles obsolete allocations when assignments change.
 */
public class FacultyModel extends BaseModel<FacultyBean> {

	private static Logger log = Logger.getLogger(FacultyModel.class);

	public FacultyBean findByEmail(String email) throws ApplicationException {
		return findByUniqueColumn("EMAIL", email);
	}

	private void resolveAssociations(FacultyBean bean) throws ApplicationException {
		if (bean.getCollegeId() > 0) {
			CollegeModel cModel = new CollegeModel();
			CollegeBean college = cModel.findByPK(bean.getCollegeId());
			if (college != null) {
				bean.setCollegeName(college.getName());
			}
		}
		if (bean.getCourseId() > 0) {
			CourseModel courseModel = new CourseModel();
			CourseBean course = courseModel.findByPK(bean.getCourseId());
			if (course != null) {
				bean.setCourseName(course.getName());
			}
		}
		if (bean.getSubjectId() > 0) {
			SubjectModel subjectModel = new SubjectModel();
			SubjectBean subject = subjectModel.findByPK(bean.getSubjectId());
			if (subject != null) {
				bean.setSubjectName(subject.getName());
				if (bean.getCourseId() <= 0 && subject.getCourseId() > 0) {
					bean.setCourseId(subject.getCourseId());
					CourseModel courseModel = new CourseModel();
					CourseBean course = courseModel.findByPK(subject.getCourseId());
					if (course != null) {
						bean.setCourseName(course.getName());
					}
				}
			}
		}
	}

	@Override
	public long add(FacultyBean bean) throws ApplicationException, DuplicateRecordException {
		log.debug("FacultyModel add Started");

		FacultyBean duplicate = findByEmail(bean.getEmail());
		if (duplicate != null) {
			throw new DuplicateRecordException("Email already exists");
		}

		resolveAssociations(bean);

		Connection conn = null;
		int pk = 0;
		try {
			pk = nextPK();
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement(
					"INSERT INTO ST_FACULTY(ID,COLLEGE_ID,COLLEGE_NAME,FIRST_NAME,LAST_NAME,EMAIL,MOBILE_NO,ADDRESS,GENDER,DATE_OF_BIRTH,CREATED_BY,MODIFIED_BY,CREATED_DATETIME,MODIFIED_DATETIME,COURSE_ID,COURSE_NAME,SUBJECT_ID,SUBJECT_NAME)"
							+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW(),?,?,?,?)");
			pstmt.setInt(1, pk);
			pstmt.setLong(2, bean.getCollegeId());
			pstmt.setString(3, bean.getCollegeName());
			pstmt.setString(4, bean.getFirstName());
			pstmt.setString(5, bean.getLastName());
			pstmt.setString(6, bean.getEmail());
			pstmt.setString(7, bean.getMobileNo());
			pstmt.setString(8, bean.getAddress());
			pstmt.setString(9, bean.getGender());
			pstmt.setDate(10, bean.getDob() != null ? new java.sql.Date(bean.getDob().getTime()) : null);
			pstmt.setString(11, bean.getCreatedBy());
			pstmt.setString(12, bean.getModifiedBy());
			pstmt.setLong(13, bean.getCourseId());
			pstmt.setString(14, bean.getCourseName());
			pstmt.setLong(15, bean.getSubjectId());
			pstmt.setString(16, bean.getSubjectName());
			pstmt.executeUpdate();
			pstmt.close();

			if (bean.getCourseId() > 0 && bean.getSubjectId() > 0) {
				FacultyAllocationModel allocModel = new FacultyAllocationModel();
				allocModel.reconcileFacultyAllocation(conn, pk, bean, 0L, 0L);
			}

			conn.commit();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in add Faculty and Allocation: " + e.getMessage());
		} finally {
			JDBCDataSource.closeConnection(conn);
		}

		log.debug("FacultyModel add End");
		return pk;
	}

	@Override
	public void update(FacultyBean bean) throws ApplicationException, DuplicateRecordException {
		log.debug("FacultyModel update Started");

		FacultyBean existing = findByEmail(bean.getEmail());
		if (existing != null && existing.getId() != bean.getId()) {
			throw new DuplicateRecordException("Email already exists");
		}

		FacultyBean priorRecord = findByPK(bean.getId());
		long previousCourseId = (priorRecord != null) ? priorRecord.getCourseId() : 0L;
		long previousSubjectId = (priorRecord != null) ? priorRecord.getSubjectId() : 0L;

		resolveAssociations(bean);

		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement(
					"UPDATE ST_FACULTY SET COLLEGE_ID=?,COLLEGE_NAME=?,FIRST_NAME=?,LAST_NAME=?,EMAIL=?,MOBILE_NO=?,ADDRESS=?,GENDER=?,DATE_OF_BIRTH=?,COURSE_ID=?,COURSE_NAME=?,SUBJECT_ID=?,SUBJECT_NAME=? WHERE ID=?");
			pstmt.setLong(1, bean.getCollegeId());
			pstmt.setString(2, bean.getCollegeName());
			pstmt.setString(3, bean.getFirstName());
			pstmt.setString(4, bean.getLastName());
			pstmt.setString(5, bean.getEmail());
			pstmt.setString(6, bean.getMobileNo());
			pstmt.setString(7, bean.getAddress());
			pstmt.setString(8, bean.getGender());
			pstmt.setDate(9, bean.getDob() != null ? new java.sql.Date(bean.getDob().getTime()) : null);
			pstmt.setLong(10, bean.getCourseId());
			pstmt.setString(11, bean.getCourseName());
			pstmt.setLong(12, bean.getSubjectId());
			pstmt.setString(13, bean.getSubjectName());
			pstmt.setLong(14, bean.getId());
			pstmt.executeUpdate();
			pstmt.close();

			updatedTimestamp(bean.getId(), conn);

			if (bean.getCourseId() > 0 && bean.getSubjectId() > 0) {
				FacultyAllocationModel allocModel = new FacultyAllocationModel();
				allocModel.reconcileFacultyAllocation(conn, bean.getId(), bean, previousCourseId, previousSubjectId);
			}

			conn.commit();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in update Faculty and Allocation: " + e.getMessage());
		} finally {
			JDBCDataSource.closeConnection(conn);
		}

		log.debug("FacultyModel update End");
	}

	@Override
	protected void buildWhereParameters(FacultyBean bean, StringBuilder sql, List<Object> params) {
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
			if (bean.getEmail() != null && !bean.getEmail().isEmpty()) {
				sql.append(" AND EMAIL LIKE ?");
				params.add(bean.getEmail() + "%");
			}
			if (bean.getCollegeId() > 0) {
				sql.append(" AND COLLEGE_ID = ?");
				params.add(bean.getCollegeId());
			}
			if (bean.getCourseId() > 0) {
				sql.append(" AND COURSE_ID = ?");
				params.add(bean.getCourseId());
			}
			if (bean.getSubjectId() > 0) {
				sql.append(" AND SUBJECT_ID = ?");
				params.add(bean.getSubjectId());
			}
		}
	}

	@Override
	public String getWhereClause(FacultyBean bean) {
		StringBuilder sql = new StringBuilder();
		if (bean != null) {
			if (bean.getId() > 0) {
				sql.append(" AND ID = ").append(bean.getId());
			}
			if (bean.getCollegeId() > 0) {
				sql.append(" AND COLLEGE_ID = ").append(bean.getCollegeId());
			}
		}
		return sql.toString();
	}

	@Override
	public String getTable() {
		return "ST_FACULTY";
	}

	@Override
	public FacultyBean getBean() {
		return new FacultyBean();
	}
}
