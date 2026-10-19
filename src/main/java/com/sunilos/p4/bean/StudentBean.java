package com.sunilos.p4.bean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Student JavaBean encapsulates Student attributes and SQL-level row-scoping
 * filters (Phase 3 Task 3).
 */
public class StudentBean extends BaseBean {

	private String firstName;
	private String lastName;
	private Date dob;
	private String mobileNo;
	private String email;
	private Long collegeId;
	private String collegeName;
	private long courseId;
	private String courseName;

	@JsonIgnore
	private Set<Long> scopedCourseIds;

	@JsonIgnore
	private Long scopedCollegeId;

	public Long getCollegeId() {
		return collegeId;
	}

	public void setCollegeId(Long collegeId) {
		this.collegeId = collegeId;
	}

	public String getCollegeName() {
		return collegeName;
	}

	public void setCollegeName(String collegeName) {
		this.collegeName = collegeName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public String getMobileNo() {
		return mobileNo;
	}

	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public long getCourseId() {
		return courseId;
	}

	public void setCourseId(long courseId) {
		this.courseId = courseId;
	}

	public String getCourseName() {
		return courseName;
	}

	public void setCourseName(String courseName) {
		this.courseName = courseName;
	}

	@JsonIgnore
	public Set<Long> getScopedCourseIds() {
		return scopedCourseIds;
	}

	@JsonIgnore
	public void setScopedCourseIds(Set<Long> scopedCourseIds) {
		this.scopedCourseIds = scopedCourseIds;
	}

	@JsonIgnore
	public Long getScopedCollegeId() {
		return scopedCollegeId;
	}

	@JsonIgnore
	public void setScopedCollegeId(Long scopedCollegeId) {
		this.scopedCollegeId = scopedCollegeId;
	}

	@Override
	public String getKey() {
		return id + "";
	}

	@Override
	public String getValue() {
		return firstName + " " + lastName;
	}

	@Override
	public void setResultset(ResultSet rs) {
		try {
			super.setResultset(rs);
			this.setCollegeId(rs.getLong(2));
			this.setCollegeName(rs.getString(3));
			this.setFirstName(rs.getString(4));
			this.setLastName(rs.getString(5));
			this.setDob(rs.getDate(6));
			this.setMobileNo(rs.getString(7));
			this.setEmail(rs.getString(8));
			try {
				long cId = rs.getLong("COURSE_ID");
				if (!rs.wasNull() && cId > 0) {
					this.setCourseId(cId);
				}
				String cName = rs.getString("COURSE_NAME");
				if (cName != null && !cName.isEmpty()) {
					this.setCourseName(cName);
				}
			} catch (SQLException ignored) {
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
