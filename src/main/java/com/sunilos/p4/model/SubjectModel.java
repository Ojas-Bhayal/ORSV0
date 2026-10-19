package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.bean.SubjectBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.JDBCDataSource;

/**
 * SubjectModel supports composite uniqueness on (COURSE_ID, NAME) (Phase 4 Task 9)
 * so subject names may repeat across different courses when valid, while rejecting
 * duplicate subject names within the same course.
 */
public class SubjectModel extends BaseModel<SubjectBean> {

	private static Logger log = Logger.getLogger(SubjectModel.class);

	public SubjectBean findByName(String name) throws ApplicationException {
		return findByUniqueColumn("NAME", name);
	}

	public SubjectBean findByCourseAndName(long courseId, String name) throws ApplicationException {
		if (name == null) {
			return null;
		}
		if (courseId <= 0) {
			return findByName(name);
		}
		String sql = "SELECT * FROM ST_SUBJECT WHERE COURSE_ID = ? AND LOWER(NAME) = ?";
		SubjectBean bean = null;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, courseId);
			pstmt.setString(2, name.trim().toLowerCase());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				bean = populateBean(rs);
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception in findByCourseAndName", e);
			throw new ApplicationException("Exception in getting Subject by Course and Name");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return bean;
	}

	public List<SubjectBean> findByCourseId(long courseId) throws ApplicationException {
		SubjectBean searchBean = new SubjectBean();
		searchBean.setCourseId(courseId);
		return search(searchBean);
	}

	private void resolveCourseName(SubjectBean bean) {
		if (bean != null && bean.getCourseId() > 0 && (bean.getCourseName() == null || bean.getCourseName().isEmpty())) {
			try {
				CourseModel courseModel = new CourseModel();
				CourseBean course = courseModel.findByPK(bean.getCourseId());
				if (course != null) {
					bean.setCourseName(course.getName());
				}
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public long add(SubjectBean bean) throws ApplicationException, DuplicateRecordException {
		String colums = "ID,NAME, DESCRIPTION, COURSE_ID";
		String values = "?,?,?,?";

		StringBuffer sql = new StringBuffer("INSERT INTO " + getTable());
		sql.append("(CREATED_DATETIME,MODIFIED_DATETIME,CREATED_BY,MODIFIED_BY, " + colums + ")");
		sql.append(" VALUES(NOW(),NOW(),'" + getDefaultUser() + "','" + getDefaultUser() + "'," + values + " )");

		checkDuplicate(bean);
		resolveCourseName(bean);

		Connection conn = null;
		int pk = 0;

		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);

			pk = nextPK();

			PreparedStatement pstmt = conn.prepareStatement(sql.toString());
			pstmt.setInt(1, pk);
			pstmt.setString(2, bean.getName());
			pstmt.setString(3, bean.getDescription());
			pstmt.setLong(4, bean.getCourseId());
			pstmt.executeUpdate();

			conn.commit();
			pstmt.close();
		} catch (SQLException e) {
			JDBCDataSource.rollBack(conn);
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return pk;
	}

	@Override
	public void update(SubjectBean bean) throws ApplicationException, DuplicateRecordException {
		String sql = "UPDATE " + getTable() + " SET NAME=?,DESCRIPTION=?,COURSE_ID=? WHERE ID=?";

		checkDuplicate(bean);
		resolveCourseName(bean);

		Connection conn = null;

		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);

			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, bean.getName());
			pstmt.setString(2, bean.getDescription());
			pstmt.setLong(3, bean.getCourseId());
			pstmt.setLong(4, bean.getId());
			pstmt.executeUpdate();

			updatedTimestamp(bean.getId(), conn);

			conn.commit();
			pstmt.close();
		} catch (SQLException e) {
			JDBCDataSource.rollBack(conn);
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	@Override
	protected void buildWhereParameters(SubjectBean bean, StringBuilder sql, List<Object> params) {
		if (bean != null) {
			if (bean.getId() > 0) {
				sql.append(" AND ID = ?");
				params.add(bean.getId());
			}
			if (bean.getName() != null && !bean.getName().isEmpty()) {
				sql.append(" AND NAME LIKE ?");
				params.add(bean.getName() + "%");
			}
			if (bean.getDescription() != null && !bean.getDescription().isEmpty()) {
				sql.append(" AND DESCRIPTION LIKE ?");
				params.add(bean.getDescription() + "%");
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
			if (bean.getScopedSubjectIds() != null && !bean.getScopedSubjectIds().isEmpty()) {
				sql.append(" AND ID IN (");
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
		}
	}

	@Override
	public String getWhereClause(SubjectBean bean) {
		StringBuilder sql = new StringBuilder();
		if (bean != null) {
			if (bean.getId() > 0) {
				sql.append(" AND ID = ").append(bean.getId());
			}
			if (bean.getCourseId() > 0) {
				sql.append(" AND COURSE_ID = ").append(bean.getCourseId());
			}
		}
		return sql.toString();
	}

	@Override
	public void checkDuplicate(SubjectBean bean) {
		SubjectBean duplicateBean = (bean.getCourseId() > 0)
				? this.findByCourseAndName(bean.getCourseId(), bean.getName())
				: this.findByName(bean.getName());
		if (duplicateBean != null && duplicateBean.getId() != bean.getId()) {
			throw new DuplicateRecordException("Subject already exists in this course");
		}
		if (bean.getId() == 0 && duplicateBean != null) {
			throw new DuplicateRecordException("Subject already exists in this course");
		}
	}

	@Override
	public String getTable() {
		return "ST_SUBJECT";
	}

	@Override
	public SubjectBean getBean() {
		return new SubjectBean();
	}
}
