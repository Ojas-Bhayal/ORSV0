package com.sunilos.p4.bean;

import java.sql.ResultSet;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.sql.SQLException;

/**
 * Subject JavaBean encapsulates Subject attributes and its parent Course mapping.
 */
public class SubjectBean extends BaseBean {

    private String name;
    private String description;
    private long courseId;
    private String courseName;

    @JsonIgnore
    private Set<Long> scopedCourseIds;

    @JsonIgnore
    private Set<Long> scopedSubjectIds;

    @JsonIgnore
    public Set<Long> getScopedCourseIds() {
        return scopedCourseIds;
    }

    public void setScopedCourseIds(Set<Long> scopedCourseIds) {
        this.scopedCourseIds = scopedCourseIds;
    }

    @JsonIgnore
    public Set<Long> getScopedSubjectIds() {
        return scopedSubjectIds;
    }

    public void setScopedSubjectIds(Set<Long> scopedSubjectIds) {
        this.scopedSubjectIds = scopedSubjectIds;
    }

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
            this.setName(rs.getString("NAME"));
            this.setDescription(rs.getString("DESCRIPTION"));
            this.setCourseId(rs.getLong("COURSE_ID"));
            try {
                String cName = rs.getString("COURSE_NAME");
                if (cName != null) {
                    this.setCourseName(cName);
                }
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
