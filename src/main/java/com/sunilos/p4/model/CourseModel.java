package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.log4j.Logger;

import com.sunilos.p4.bean.CourseBean;
import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.exception.DuplicateRecordException;
import com.sunilos.p4.util.JDBCDataSource;

public class CourseModel extends BaseModel<CourseBean> {

    private static Logger log = Logger.getLogger(CourseModel.class);

    public CourseBean findByName(String name) throws ApplicationException {
        return findByUniqueColumn("NAME", name);
    }

    @Override
    public long add(CourseBean bean) throws ApplicationException, DuplicateRecordException {
        String colums = "ID,NAME, DESCRIPTION, DURATION";
        String values = "?,?,?,?";

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
            pstmt.setString(4, bean.getDuration());
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
    public void update(CourseBean bean) throws ApplicationException, DuplicateRecordException {
        String sql = "UPDATE " + getTable() + " SET NAME=?,DESCRIPTION=?,DURATION=? WHERE ID=?";

        checkDuplicate(bean);

        Connection conn = null;

        try {
            conn = JDBCDataSource.getConnection();
            conn.setAutoCommit(false);

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, bean.getName());
            pstmt.setString(2, bean.getDescription());
            pstmt.setString(3, bean.getDuration());
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
    protected void buildWhereParameters(CourseBean bean, StringBuilder sql, List<Object> params) {
        if (bean != null) {
            if (bean.getId() > 0) {
                sql.append(" AND ID = ?");
                params.add(bean.getId());
            }
            if (bean.getScopedCourseIds() != null && !bean.getScopedCourseIds().isEmpty()) {
                sql.append(" AND ID IN (");
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
            if (bean.getName() != null && !bean.getName().isEmpty()) {
                sql.append(" AND NAME LIKE ?");
                params.add(bean.getName() + "%");
            }
            if (bean.getDescription() != null && !bean.getDescription().isEmpty()) {
                sql.append(" AND DESCRIPTION LIKE ?");
                params.add(bean.getDescription() + "%");
            }
            if (bean.getDuration() != null && !bean.getDuration().isEmpty()) {
                sql.append(" AND DURATION LIKE ?");
                params.add(bean.getDuration() + "%");
            }
        }
    }

    @Override
    public String getWhereClause(CourseBean bean) {
        StringBuilder sql = new StringBuilder();
        if (bean != null && bean.getId() > 0) {
            sql.append(" AND ID = ").append(bean.getId());
        }
        return sql.toString();
    }

    @Override
    public void checkDuplicate(CourseBean bean) {
        CourseBean duplicateBean = this.findByName(bean.getName());
        if (duplicateBean != null && duplicateBean.getId() != bean.getId()) {
            throw new DuplicateRecordException("Course already exists");
        }
        if (bean.getId() == 0 && duplicateBean != null) {
            throw new DuplicateRecordException("Course already exists");
        }
    }

    @Override
    public String getTable() {
        return "ST_COURSE";
    }

    @Override
    public CourseBean getBean() {
        return new CourseBean();
    }
}
