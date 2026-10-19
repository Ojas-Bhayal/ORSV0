package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CollegeBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.JDBCDataSource;

public class CollegeModel extends BaseModel<CollegeBean> {

	private static Logger log = Logger.getLogger(CollegeModel.class);

	@Override
	public long add(CollegeBean bean) throws ApplicationException, DuplicateRecordException {
		log.debug("Model add Started");
		Connection conn = null;
		int pk = 0;

		CollegeBean duplicateCollegeName = findByName(bean.getName());
		if (duplicateCollegeName != null) {
			throw new DuplicateRecordException("College Name already exists");
		}

		try {
			conn = JDBCDataSource.getConnection();
			pk = nextPK();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement("INSERT INTO ST_COLLEGE VALUES(?,?,?,?,?,?,?,?,?,?)");
			pstmt.setInt(1, pk);
			pstmt.setString(2, bean.getName());
			pstmt.setString(3, bean.getAddress());
			pstmt.setString(4, bean.getState());
			pstmt.setString(5, bean.getCity());
			pstmt.setString(6, bean.getPhoneNo());
			pstmt.setString(7, bean.getCreatedBy());
			pstmt.setString(8, bean.getModifiedBy());
			pstmt.setTimestamp(9, bean.getCreatedDatetime());
			pstmt.setTimestamp(10, bean.getModifiedDatetime());
			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception : Exception in add College");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model add End");
		return pk;
	}

	public CollegeBean findByName(String name) throws ApplicationException {
		return findByUniqueColumn("NAME", name);
	}

	@Override
	public void update(CollegeBean bean) throws ApplicationException, DuplicateRecordException {
		log.debug("Model update Started");
		Connection conn = null;

		CollegeBean beanExist = findByName(bean.getName());
		if (beanExist != null && beanExist.getId() != bean.getId()) {
			throw new DuplicateRecordException("College is already exist");
		}

		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement(
					"UPDATE ST_COLLEGE SET NAME=?,ADDRESS=?,STATE=?,CITY=?,PHONE_NO=?,CREATED_BY=?,MODIFIED_BY=?,CREATED_DATETIME=?,MODIFIED_DATETIME=? WHERE ID=?");
			pstmt.setString(1, bean.getName());
			pstmt.setString(2, bean.getAddress());
			pstmt.setString(3, bean.getState());
			pstmt.setString(4, bean.getCity());
			pstmt.setString(5, bean.getPhoneNo());
			pstmt.setString(6, bean.getCreatedBy());
			pstmt.setString(7, bean.getModifiedBy());
			pstmt.setTimestamp(8, bean.getCreatedDatetime());
			pstmt.setTimestamp(9, bean.getModifiedDatetime());
			pstmt.setLong(10, bean.getId());
			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			JDBCDataSource.rollBack(conn);
			throw new ApplicationException("Exception in updating College ");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model update End");
	}

	@Override
	protected void buildWhereParameters(CollegeBean bean, StringBuilder sql, List<Object> params) {
		if (bean != null) {
			if (bean.getId() > 0) {
				sql.append(" AND ID = ?");
				params.add(bean.getId());
			}
			if (bean.getName() != null && !bean.getName().isEmpty()) {
				sql.append(" AND NAME LIKE ?");
				params.add(bean.getName() + "%");
			}
			if (bean.getAddress() != null && !bean.getAddress().isEmpty()) {
				sql.append(" AND ADDRESS LIKE ?");
				params.add(bean.getAddress() + "%");
			}
			if (bean.getState() != null && !bean.getState().isEmpty()) {
				sql.append(" AND STATE LIKE ?");
				params.add(bean.getState() + "%");
			}
			if (bean.getCity() != null && !bean.getCity().isEmpty()) {
				sql.append(" AND CITY LIKE ?");
				params.add(bean.getCity() + "%");
			}
			if (bean.getPhoneNo() != null && !bean.getPhoneNo().isEmpty()) {
				sql.append(" AND PHONE_NO = ?");
				params.add(bean.getPhoneNo());
			}
		}
	}

	@Override
	public String getTable() {
		return "ST_COLLEGE";
	}

	@Override
	public CollegeBean getBean() {
		return new CollegeBean();
	}

	@Override
	public String getWhereClause(CollegeBean bean) {
		StringBuilder sql = new StringBuilder();
		if (bean != null && bean.getId() > 0) {
			sql.append(" AND ID = ").append(bean.getId());
		}
		return sql.toString();
	}

}
