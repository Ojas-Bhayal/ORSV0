package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.RoleBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.JDBCDataSource;

public class RoleModel extends BaseModel<RoleBean> {

	private static Logger log = Logger.getLogger(RoleModel.class);

	public RoleBean findByName(String name) throws ApplicationException {
		return findByUniqueColumn("NAME", name);
	}

	@Override
	public long add(RoleBean bean) throws ApplicationException, DuplicateRecordException {
		String colums = "ID,NAME, DESCRIPTION";
		String values = "?,?,?";

		StringBuffer sql = new StringBuffer("INSERT INTO " + getTable());
		sql.append("(CREATED_DATETIME,MODIFIED_DATETIME,CREATED_BY,MODIFIED_BY, " + colums + ")");
		sql.append(" VALUES(NOW(),NOW(),'" + getDefaultUser() + "','" + getDefaultUser() + "'," + values + " )");

		checkDuplicate(bean);

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
	public void update(RoleBean bean) throws ApplicationException, DuplicateRecordException {
		String sql = "UPDATE " + getTable() + " SET NAME=?,DESCRIPTION=? WHERE ID=?";

		checkDuplicate(bean);

		Connection conn = null;

		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);

			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, bean.getName());
			pstmt.setString(2, bean.getDescription());
			pstmt.setLong(3, bean.getId());
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
	protected void buildWhereParameters(RoleBean bean, StringBuilder sql, List<Object> params) {
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
		}
	}

	@Override
	public String getWhereClause(RoleBean bean) {
		StringBuilder sql = new StringBuilder();
		if (bean != null && bean.getId() > 0) {
			sql.append(" AND ID = ").append(bean.getId());
		}
		return sql.toString();
	}

	@Override
	public void checkDuplicate(RoleBean bean) {
		RoleBean duplicateBean = this.findByName(bean.getName());
		if (duplicateBean != null && duplicateBean.getId() != bean.getId()) {
			throw new DuplicateRecordException("Role already exists");
		}
		if (bean.getId() == 0 && duplicateBean != null) {
			throw new DuplicateRecordException("Role already exists");
		}
	}

	@Override
	public String getTable() {
		return "ST_ROLE";
	}

	@Override
	public RoleBean getBean() {
		return new RoleBean();
	}
}
