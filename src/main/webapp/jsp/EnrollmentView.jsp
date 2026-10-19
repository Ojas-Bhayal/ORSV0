<%@page import="com.sunilos.p4.ctl.EnrollmentCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.util.HTMLUtility"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="java.util.List"%>
<jsp:useBean id="bean" class="com.sunilos.p4.bean.EnrollmentBean" scope="request"></jsp:useBean>
<%
    List studentList = (List) request.getAttribute("studentList");
    List courseList = (List) request.getAttribute("courseList");
%>
<div class="container py-4">
  <h3><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId() > 0 ? "Update Student Enrollment" : "Add Student Enrollment")%></h3>
  <H4><font color="green"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getSuccessMessage(request))%></font></H4>
  <H4><font color="red"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage(request))%></font></H4>
  <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.ENROLLMENT_CTL)%>" method="post">
  <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
    <input type="hidden" name="id" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>">
    <table class="table table-borderless" style="max-width:650px;">
      <tr>
        <th>Student <span style="color:red">*</span></th>
        <td><%=HTMLUtility.getList("studentId", String.valueOf(bean.getStudentId()), studentList)%></td>
        <td><font color="red"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("studentId", request))%></font></td>
      </tr>
      <tr>
        <th>Course <span style="color:red">*</span></th>
        <td><%=HTMLUtility.getList("courseId", String.valueOf(bean.getCourseId()), courseList)%></td>
        <td><font color="red"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("courseId", request))%></font></td>
      </tr>
      <tr>
        <th>Roll Number <span style="color:red">*</span></th>
        <td><input type="text" name="rollNo" class="form-control" required value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getRollNo()))%>"></td>
        <td><font color="red"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("rollNo", request))%></font></td>
      </tr>
      <tr>
        <th>Semester</th>
        <td><input type="text" name="semester" class="form-control" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getSemester()))%>"></td>
        <td></td>
      </tr>
      <tr>
        <th>Status</th>
        <td>
          <select name="status" class="form-select">
            <option value="ACTIVE" <%=com.sunilos.p4.util.HTMLUtility.escapeHtml("ACTIVE".equalsIgnoreCase(bean.getStatus()) ? "selected" : "")%>>ACTIVE</option>
            <option value="INACTIVE" <%=com.sunilos.p4.util.HTMLUtility.escapeHtml("INACTIVE".equalsIgnoreCase(bean.getStatus()) ? "selected" : "")%>>INACTIVE</option>
          </select>
        </td>
        <td></td>
      </tr>
      <tr>
        <th></th>
        <td>
          <input type="submit" name="operation" class="btn btn-primary" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(EnrollmentCtl.OP_SAVE)%>">
          <input type="submit" name="operation" class="btn btn-secondary" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(EnrollmentCtl.OP_CANCEL)%>">
        </td>
      </tr>
    </table>
  </form>
</div>
