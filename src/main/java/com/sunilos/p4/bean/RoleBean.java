package com.sunilos.p4.bean;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Role JavaBean encapsulates Role attributes matching SE Experiment 1 & 3 Actors:
 * Administrator, Student, Program Chair / HOD, Faculty, College, KIOSK.
 * 
 * @author Rays Technologies
 * @version 2.0
 * @Copyright (c) Rays Technologies
 */
public class RoleBean extends BaseBean {

	/**
	 * Predefined Role constants aligned with SE Experiment 1, 3, 4, 6
	 */
	public static final int ADMIN = 1;
	public static final int STUDENT = 2;
	public static final int PROGRAM_CHAIR = 3;
	public static final int FACULTY = 4;
	public static final int COLLEGE_SCHOOL = 5;
	public static final int KIOSK = 6;

	/**
	 * Role Name
	 */
	private String name;

	/**
	 * Role Description
	 */
	private String description;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getKey() {
		return id + "";
	}

	@Override
	public String getValue() {
		return name;
	}

	@Override
	public void setResultset(ResultSet rs) {
		try {
			super.setResultset(rs);
			this.setName(rs.getString(2));
			this.setDescription(rs.getString(3));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
