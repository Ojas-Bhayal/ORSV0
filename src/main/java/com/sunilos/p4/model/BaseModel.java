package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.BaseBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DatabaseException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.DataUtility;
import com.sunilos.p4.util.DataValidator;
import com.sunilos.p4.util.JDBCDataSource;
import com.sunilos.p4.util.PropertyReader;

/**
 * Generalized abstract superclass for all Model classes in ORS.
 * Audit High Fix #5: All SQL queries use parameterized PreparedStatement
 * bindings (?) to eliminate SQL injection across findByUniqueColumn,
 * updatedTimestamp, and search().
 * 
 * @author Sunil Sahu
 * @param <T>
 */
public abstract class BaseModel<T extends BaseBean> implements Comparable<BaseModel<T>> {

	protected static Logger log = Logger.getLogger(BaseModel.class);

	protected long id;
	protected String createdBy;
	protected String modifiedBy;
	protected Timestamp createdDatetime;
	protected Timestamp modifiedDatetime;
	protected int defaultPageSize = DataUtility.getInt(PropertyReader.getValue("page.size"));

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public Timestamp getCreatedDatetime() {
		return createdDatetime;
	}

	public void setCreatedDatetime(Timestamp createdDatetime) {
		this.createdDatetime = createdDatetime;
	}

	public Timestamp getModifiedDatetime() {
		return modifiedDatetime;
	}

	public void setModifiedDatetime(Timestamp modifiedDatetime) {
		this.modifiedDatetime = modifiedDatetime;
	}

	@Override
	public int compareTo(BaseModel<T> other) {
		return Long.compare(this.id, other != null ? other.id : 0L);
	}

	protected String getDefaultUser() {
		String user = PropertyReader.getValue("default.user");
		return (user != null && !user.equals("default.user")) ? user : "root@sunilos.com";
	}

	/**
	 * Parameterized timestamp update (prevents SQL injection).
	 */
	protected void updatedTimestamp(long id, Connection conn) throws SQLException {
		String modUser = (this.modifiedBy != null && !this.modifiedBy.isEmpty()) ? this.modifiedBy : getDefaultUser();
		String sql = "UPDATE " + getTable() + " SET MODIFIED_DATETIME = NOW(), MODIFIED_BY = ? WHERE ID = ?";
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, modUser);
			pstmt.setLong(2, id);
			pstmt.executeUpdate();
		}
	}

	/**
	 * Populates a Bean instance from a JDBC ResultSet (Experiment 5 Class Diagram).
	 */
	protected T populateBean(ResultSet rs) {
		T bean = getBean();
		bean.setResultset(rs);
		return bean;
	}

	public Integer nextPK() throws DatabaseException {
		log.debug("Model nextPK Started");
		Connection conn = null;
		int pk = 0;

		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement("SELECT MAX(ID) FROM " + getTable());
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				pk = rs.getInt(1);
			}
			rs.close();
			pstmt.close();
		} catch (SQLException e) {
			log.error("Database Exception..", e);
			throw new DatabaseException("Exception : Exception in getting PK");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model nextPK End");
		return pk + 1;
	}

	public T findByPK(long pk) throws ApplicationException {
		log.debug("Model findByPK Started");
		String sql = "SELECT * FROM " + getTable() + " WHERE ID=?";
		T bean = null;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setLong(1, pk);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				bean = populateBean(rs);
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			throw new ApplicationException("Exception : Exception in getting record by pk");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model findByPK End");
		return bean;
	}

	/**
	 * Parameterized lookup by unique column (Audit High Fix #5: eliminates SQL injection).
	 */
	public T findByUniqueColumn(String column, String value) throws ApplicationException {
		log.debug("Model findByUniqueColumn Started");
		if (!DataValidator.isSafeSqlIdentifier(column)) {
			throw new ApplicationException("Invalid column identifier: " + column);
		}

		String sql = "SELECT * FROM " + getTable() + " WHERE " + column + " = ?";
		T bean = null;
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, value);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				bean = populateBean(rs);
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			throw new ApplicationException("Exception : Exception in getting record by " + column);
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model findByUniqueColumn End");
		return bean;
	}

	public abstract long add(T bean) throws ApplicationException, DuplicateRecordException;

	public abstract void update(T bean) throws ApplicationException, DuplicateRecordException;

	public void delete(T bean) throws ApplicationException {
		log.debug("Model delete Started");
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + getTable() + " WHERE ID=?");
			pstmt.setLong(1, bean.getId());
			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			try {
				conn.rollback();
			} catch (Exception ex) {
				throw new ApplicationException("Exception : Delete rollback exception " + ex.getMessage());
			}
			throw new ApplicationException("Exception : Exception in delete record");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		log.debug("Model delete Ended");
	}

	public void delete(int id) throws ApplicationException {
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			conn.setAutoCommit(false);
			PreparedStatement pstmt = conn.prepareStatement("DELETE FROM " + getTable() + " WHERE ID=?");
			pstmt.setLong(1, id);
			pstmt.executeUpdate();
			conn.commit();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			try {
				conn.rollback();
			} catch (Exception ex) {
				throw new ApplicationException("Exception : Delete rollback exception " + ex.getMessage());
			}
			throw new ApplicationException("Exception : Exception in delete record");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
	}

	public List<T> search(T bean) throws ApplicationException {
		return search(bean, 0, 0);
	}

	/**
	 * Parameterized search query builder. Subclasses override this to append
	 * parameterized WHERE conditions (" AND COL LIKE ?") and add parameter values
	 * to params.
	 */
	protected void buildWhereParameters(T bean, StringBuilder sql, List<Object> params) {
	}

	/**
	 * Executes parameterized search with pagination (Audit High Fix #5).
	 */
	public List<T> search(T bean, int pageNo, int pageSize) throws ApplicationException {
		log.debug("Model search Started");
		StringBuilder sql = new StringBuilder("SELECT * FROM " + getTable() + " WHERE 1=1");
		List<Object> params = new ArrayList<>();
		buildWhereParameters(bean, sql, params);

		if (pageSize > 0) {
			int offset = (pageNo - 1) * pageSize;
			sql.append(" LIMIT ?, ?");
			params.add(offset);
			params.add(pageSize);
		}

		ArrayList<T> list = new ArrayList<T>();
		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql.toString());
			for (int i = 0; i < params.size(); i++) {
				pstmt.setObject(i + 1, params.get(i));
			}
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				T rowBean = populateBean(rs);
				list.add(rowBean);
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			throw new ApplicationException("Exception : Exception in search");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}

		log.debug("Model search End");
		return list;
	}

	public List<T> list() throws ApplicationException {
		return list(0, 0);
	}

	public List<T> list(int pageNo, int pageSize) throws ApplicationException {
		log.debug("Model list Started");
		ArrayList<T> list = new ArrayList<T>();
		StringBuilder sql = new StringBuilder("SELECT * FROM " + getTable());
		List<Object> params = new ArrayList<>();

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
				T bean = populateBean(rs);
				list.add(bean);
			}
			rs.close();
			pstmt.close();
		} catch (Exception e) {
			log.error("Database Exception..", e);
			throw new ApplicationException("Exception : Exception in getting list");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}

		log.debug("Model list End");
		return list;
	}

	public abstract String getWhereClause(T bean);

	public abstract String getTable();

	public abstract T getBean();

	public void checkDuplicate(T bean) {
	}

}
