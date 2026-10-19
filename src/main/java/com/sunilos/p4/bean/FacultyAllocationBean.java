package com.sunilos.p4.bean;

import java.sql.ResultSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.SQLException;

/**
 * FacultyAllocationBean models many-to-many Faculty-to-Course/Subject teaching
 * allocations stored in ST_FACULTY_ALLOCATION (Audit High Fix #4 & SE
 * Experiments 1, 4, 5, 8).
 */
public class FacultyAllocationBean extends BaseBean {

	private long facultyId;
	private String facultyName;
	private String facultyEmail;
	private long courseId;
	private String courseName;
	private long subjectId;
	private String subjectName;

	@JsonIgnore
	private Set<Long> scopedCourseIds;

	@JsonIgnore
	private Long scopedCollegeId;

	@JsonIgnore
	public Set<Long> getScopedCourseIds() {
		return scopedCourseIds;
	}

	public void setScopedCourseIds(Set<Long> scopedCourseIds) {
		this.scopedCourseIds = scopedCourseIds;
	}

	@JsonIgnore
	public Long getScopedCollegeId() {
		return scopedCollegeId;
	}

	public void setScopedCollegeId(Long scopedCollegeId) {
		this.scopedCollegeId = scopedCollegeId;
	}

	public long getFacultyId() {
		return facultyId;
	}

	public void setFacultyId(long facultyId) {
		this.facultyId = facultyId;
	}

	public String getFacultyName() {
		return facultyName;
	}

	public void setFacultyName(String facultyName) {
		this.facultyName = facultyName;
	}

	public String getFacultyEmail() {
		return facultyEmail;
	}

	public void setFacultyEmail(String facultyEmail) {
		this.facultyEmail = facultyEmail;
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

	public long getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(long subjectId) {
		this.subjectId = subjectId;
	}

	public String getSubjectName() {
		return subjectName;
	}

	public void setSubjectName(String subjectName) {
		this.subjectName = subjectName;
	}

	@Override
	public String getKey() {
		return id + "";
	}

	@Override
	public String getValue() {
		return (facultyName != null ? facultyName : "") + " - " + (courseName != null ? courseName : "") + " ("
				+ (subjectName != null ? subjectName : "") + ")";
	}

	@Override
	public void setResultset(ResultSet rs) {
		try {
			super.setResultset(rs);
			this.facultyId = rs.getLong("FACULTY_ID");
			this.facultyName = rs.getString("FACULTY_NAME");
			this.facultyEmail = rs.getString("FACULTY_EMAIL");
			this.courseId = rs.getLong("COURSE_ID");
			this.courseName = rs.getString("COURSE_NAME");
			this.subjectId = rs.getLong("SUBJECT_ID");
			this.subjectName = rs.getString("SUBJECT_NAME");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
