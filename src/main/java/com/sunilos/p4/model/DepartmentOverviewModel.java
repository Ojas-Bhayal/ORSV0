package com.sunilos.p4.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.sunilos.p4.exception.ApplicationException;
import com.sunilos.p4.util.JDBCDataSource;

/**
 * Departmental Overview & Enrollment Summary Model (Phase 3 Schema Standardization).
 * Uses the single canonical ST_ENROLLMENT (COURSE_ID, SEMESTER, STATUS='ACTIVE')
 * and ST_MARKSHEET (COURSE_ID, SUBJECT_ID, MARKS) schema.
 */
public class DepartmentOverviewModel {

	private static Logger log = Logger.getLogger(DepartmentOverviewModel.class);

	/**
	 * Returns course/semester enrollment headcounts from the canonical ST_ENROLLMENT table.
	 */
	public List<Map<String, Object>> getEnrollmentSummaries(long scopedCollegeId) throws ApplicationException {
		List<Map<String, Object>> summaries = new ArrayList<>();
		StringBuilder sql = new StringBuilder(
				"SELECT e.COURSE_ID, c.NAME AS COURSE_NAME, COALESCE(e.SEMESTER, 'Sem 1') AS SEMESTER, COUNT(e.ID) AS ACTIVE_COUNT "
						+ "FROM ST_ENROLLMENT e "
						+ "INNER JOIN ST_COURSE c ON e.COURSE_ID = c.ID "
						+ "INNER JOIN ST_STUDENT s ON e.STUDENT_ID = s.ID "
						+ "WHERE e.STATUS = 'ACTIVE'");
		if (scopedCollegeId > 0) {
			sql.append(" AND s.COLLEGE_ID = ?");
		}
		sql.append(" GROUP BY e.COURSE_ID, c.NAME, e.SEMESTER ORDER BY e.COURSE_ID ASC, e.SEMESTER ASC");

		Connection conn = null;
		try {
			conn = JDBCDataSource.getConnection();
			try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
				if (scopedCollegeId > 0) {
					pstmt.setLong(1, scopedCollegeId);
				}
				try (ResultSet rs = pstmt.executeQuery()) {
					while (rs.next()) {
						Map<String, Object> row = new LinkedHashMap<>();
						row.put("courseId", rs.getLong("COURSE_ID"));
						row.put("courseName", rs.getString("COURSE_NAME"));
						row.put("semester", rs.getString("SEMESTER"));
						row.put("activeEnrollmentCount", rs.getInt("ACTIVE_COUNT"));
						summaries.add(row);
					}
				}
			}
		} catch (Exception e) {
			log.error("Database Exception in getEnrollmentSummaries", e);
			throw new ApplicationException("Exception in getting departmental enrollment summaries");
		} finally {
			JDBCDataSource.closeConnection(conn);
		}
		return summaries;
	}
}
