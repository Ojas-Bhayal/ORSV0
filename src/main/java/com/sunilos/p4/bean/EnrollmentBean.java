package com.sunilos.p4.bean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * EnrollmentBean models many-to-many Student-to-Course enrollments stored in
 * ST_ENROLLMENT (Folder3 Audit P0 Fix #2: SQL-level row scoping).
 */
public class EnrollmentBean extends BaseBean {

	private long studentId;
	private String studentName;
	private String rollNo;
	private long courseId;
	private String courseName;
	private String semester = "Sem 1";
	private String status = "ACTIVE";

	@JsonIgnore
	private Set<Long> scopedCourseIds;

	@JsonIgnore
	private Long scopedCollegeId;

	@JsonIgnore
	private Long scopedStudentId;

	public long getStudentId() {
		return studentId;
	}

	public void setStudentId(long studentId) {
		this.studentId = studentId;
	}

	public String getStudentName() {
		return studentName;
	}

	public void setStudentName(String studentName) {
		this.studentName = studentName;
	}

	public String getRollNo() {
		return rollNo;
	}

	public void setRollNo(String rollNo) {
		this.rollNo = rollNo;
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

	public String getSemester() {
		return semester;
	}

	public void setSemester(String semester) {
		this.semester = semester;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

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

	@JsonIgnore
	public Long getScopedStudentId() {
		return scopedStudentId;
	}

	public void setScopedStudentId(Long scopedStudentId) {
		this.scopedStudentId = scopedStudentId;
	}

	@Override
	public String getKey() {
		return id + "";
	}

	@Override
	public String getValue() {
		return (studentName != null ? studentName : "") + " - " + (courseName != null ? courseName : "");
	}

	@Override
	public void setResultset(ResultSet rs) {
		try {
			super.setResultset(rs);
			this.studentId = rs.getLong("STUDENT_ID");
			this.studentName = rs.getString("STUDENT_NAME");
			this.rollNo = rs.getString("ROLL_NO");
			this.courseId = rs.getLong("COURSE_ID");
			this.courseName = rs.getString("COURSE_NAME");
			this.semester = rs.getString("SEMESTER");
			this.status = rs.getString("STATUS");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
