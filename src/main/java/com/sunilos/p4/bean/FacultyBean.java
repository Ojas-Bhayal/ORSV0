package com.sunilos.p4.bean;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * Faculty JavaBean encapsulates Faculty attributes without hardcoded course/subject defaults.
 */
public class FacultyBean extends BaseBean {

    private long   collegeId;
    private String collegeName;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNo;
    private String address;
    private String gender;
    private Date   dob;
    private long   courseId;
    private String courseName;
    private long   subjectId;
    private String subjectName;

    public long getCollegeId()              { return collegeId; }
    public void setCollegeId(long collegeId){ this.collegeId = collegeId; }

    public String getCollegeName()              { return collegeName; }
    public void setCollegeName(String collegeName){ this.collegeName = collegeName; }

    public String getFirstName()              { return firstName; }
    public void setFirstName(String firstName){ this.firstName = firstName; }

    public String getLastName()             { return lastName; }
    public void setLastName(String lastName){ this.lastName = lastName; }

    public String getEmail()          { return email; }
    public void setEmail(String email){ this.email = email; }

    public String getMobileNo()             { return mobileNo; }
    public void setMobileNo(String mobileNo){ this.mobileNo = mobileNo; }

    public String getAddress()            { return address; }
    public void setAddress(String address){ this.address = address; }

    public String getGender()           { return gender; }
    public void setGender(String gender){ this.gender = gender; }

    public Date getDob()        { return dob; }
    public void setDob(Date dob){ this.dob = dob; }

    public long getCourseId()             { return courseId; }
    public void setCourseId(long courseId){ this.courseId = courseId; }

    public String getCourseName()               { return courseName; }
    public void setCourseName(String courseName){ this.courseName = courseName; }

    public long getSubjectId()              { return subjectId; }
    public void setSubjectId(long subjectId){ this.subjectId = subjectId; }

    public String getSubjectName()                { return subjectName; }
    public void setSubjectName(String subjectName){ this.subjectName = subjectName; }

    @Override
    public String getKey()   { return id + ""; }

    @Override
    public String getValue() { return firstName + " " + lastName; }

    @Override
    public void setResultset(ResultSet rs) {
        try {
            super.setResultset(rs);
            this.setCollegeId(rs.getLong(2));
            this.setCollegeName(rs.getString(3));
            this.setFirstName(rs.getString(4));
            this.setLastName(rs.getString(5));
            this.setEmail(rs.getString(6));
            this.setMobileNo(rs.getString(7));
            this.setAddress(rs.getString(8));
            this.setGender(rs.getString(9));
            this.setDob(rs.getDate(10));
            try {
                this.setCourseId(rs.getLong("COURSE_ID"));
                this.setCourseName(rs.getString("COURSE_NAME"));
                this.setSubjectId(rs.getLong("SUBJECT_ID"));
                this.setSubjectName(rs.getString("SUBJECT_NAME"));
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
