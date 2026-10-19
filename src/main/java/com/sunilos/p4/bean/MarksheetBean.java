package com.sunilos.p4.bean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Marksheet JavaBean encapsulates pure Course-Wise and Subject-Wise marks
 * without legacy Physics/Chemistry/Maths columns or hardcoded Java/String defaults
 * (Phase 4 & Phase 5).
 */
public class MarksheetBean extends BaseBean {

	private String rollNo;
	private Long studentId;
	private String name;
	private long courseId;
	private String courseName;
	private long subjectId;
	private String subjectName;
	private Integer marks;

	/**
	 * Map of Subject Name -> Marks obtained by the student in this Course.
	 */
	private Map<String, Integer> subjectMarks = new LinkedHashMap<>();

	/**
	 * Map of Subject Name -> Subject-specific Class Average for this Course.
	 */
	private Map<String, Double> subjectClassAverages = new LinkedHashMap<>();

	private double classAverage;

	@JsonIgnore
	private Set<Long> scopedCourseIds;
	@JsonIgnore
	private Set<Long> scopedSubjectIds;
	@JsonIgnore
	private Long scopedStudentId;

	@JsonIgnore
	private Long scopedCollegeId;

	@JsonIgnore
	public Long getScopedCollegeId() {
		return scopedCollegeId;
	}

	public void setScopedCollegeId(Long scopedCollegeId) {
		this.scopedCollegeId = scopedCollegeId;
	}
	@JsonIgnore
	private String actorFacultyEmail;

	public String getRollNo() {
		return rollNo;
	}

	public void setRollNo(String rollNo) {
		this.rollNo = rollNo;
	}

	public Long getStudentId() {
		return studentId;
	}

	public void setStudentId(Long studentId) {
		this.studentId = studentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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

	public Integer getMarks() {
		return marks;
	}

	public void setMarks(Integer marks) {
		this.marks = marks;
	}

	public Map<String, Integer> getSubjectMarks() {
		return subjectMarks;
	}

	public void setSubjectMarks(Map<String, Integer> subjectMarks) {
		this.subjectMarks = subjectMarks != null ? subjectMarks : new LinkedHashMap<>();
	}

	public void addSubjectMark(String subject, Integer mark) {
		if (subject != null && !subject.trim().isEmpty() && mark != null) {
			this.subjectMarks.put(subject.trim(), mark);
		}
	}

	public Map<String, Double> getSubjectClassAverages() {
		return subjectClassAverages;
	}

	public void setSubjectClassAverages(Map<String, Double> subjectClassAverages) {
		this.subjectClassAverages = subjectClassAverages != null ? subjectClassAverages : new LinkedHashMap<>();
	}

	public void addSubjectClassAverage(String subject, Double avg) {
		if (subject != null && !subject.trim().isEmpty() && avg != null) {
			this.subjectClassAverages.put(subject.trim(), avg);
		}
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
	public Set<Long> getScopedSubjectIds() {
		return scopedSubjectIds;
	}

	@JsonIgnore
	public void setScopedSubjectIds(Set<Long> scopedSubjectIds) {
		this.scopedSubjectIds = scopedSubjectIds;
	}

	@JsonIgnore
	public Long getScopedStudentId() {
		return scopedStudentId;
	}

	@JsonIgnore
	public void setScopedStudentId(Long scopedStudentId) {
		this.scopedStudentId = scopedStudentId;
	}

	@JsonIgnore
	public String getActorFacultyEmail() {
		return actorFacultyEmail;
	}

	@JsonIgnore
	public void setActorFacultyEmail(String actorFacultyEmail) {
		this.actorFacultyEmail = actorFacultyEmail;
	}

	public double getClassAverage() {
		return classAverage;
	}

	public void setClassAverage(double classAverage) {
		this.classAverage = classAverage;
	}

	public int getTotalCourseMarks() {
		if (!subjectMarks.isEmpty()) {
			int sum = 0;
			for (Integer m : subjectMarks.values()) {
				if (m != null) {
					sum += m;
				}
			}
			return sum;
		}
		return marks != null ? marks : 0;
	}

	public int getMaxCourseMarks() {
		int count = !subjectMarks.isEmpty() ? subjectMarks.size() : 1;
		return count * 100;
	}

	public double getPercentage() {
		return getCoursePercentage();
	}

	public double getCoursePercentage() {
		int max = getMaxCourseMarks();
		return max > 0 ? (getTotalCourseMarks() * 100.0) / max : 0.0;
	}

	@Override
	public String getKey() {
		return id + "";
	}

	@Override
	public String getValue() {
		return rollNo + " - " + (name != null ? name : "")
				+ (courseName != null ? " (" + courseName + ")" : "");
	}

	@Override
	public void setResultset(ResultSet rs) {
		try {
			super.setResultset(rs);
			this.rollNo = rs.getString("ROLL_NO");
			this.studentId = rs.getLong("STUDENT_ID");
			this.name = rs.getString("NAME");
			this.courseId = rs.getLong("COURSE_ID");
			this.courseName = rs.getString("COURSE_NAME");
			this.subjectId = rs.getLong("SUBJECT_ID");
			this.subjectName = rs.getString("SUBJECT_NAME");
			int m = rs.getInt("MARKS");
			if (!rs.wasNull()) {
				this.marks = m;
				if (this.subjectName != null) {
					this.subjectMarks.put(this.subjectName, m);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
