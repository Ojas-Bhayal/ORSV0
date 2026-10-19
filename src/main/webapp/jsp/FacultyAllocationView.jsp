<%@page import="com.sunilos.p4.ctl.FacultyAllocationCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.util.HTMLUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="java.util.List"%>
<jsp:useBean id="bean" class="com.sunilos.p4.bean.FacultyAllocationBean" scope="request"></jsp:useBean>
<%
    List facultyList = (List) request.getAttribute("facultyList");
    List courseList = (List) request.getAttribute("courseList");
    List subjectList = (List) request.getAttribute("subjectList");
%>
<div class="container py-4">
  <h3><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId() > 0 ? "Update Faculty Allocation" : "Add Faculty Allocation")%></h3>
  <H4><font color="green"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getSuccessMessage(request))%></font></H4>
  <H4><font color="red"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage(request))%></font></H4>
  <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.FACULTY_ALLOCATION_CTL)%>" method="post">
  <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
    <input type="hidden" name="id" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>">
    <table class="table table-borderless" style="max-width:650px;">
      <tr>
        <th>Faculty <span style="color:red">*</span></th>
        <td><%=HTMLUtility.getList("facultyId", String.valueOf(bean.getFacultyId()), facultyList)%></td>
        <td><font color="red"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("facultyId", request))%></font></td>
      </tr>
      <tr>
        <th>Course <span style="color:red">*</span></th>
        <td><%=HTMLUtility.getList("courseId", String.valueOf(bean.getCourseId()), courseList)%></td>
        <td><font color="red"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("courseId", request))%></font></td>
      </tr>
      <tr>
        <th>Subject <span style="color:red">*</span></th>
        <td><%=HTMLUtility.getList("subjectId", String.valueOf(bean.getSubjectId()), subjectList)%></td>
        <td><font color="red"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("subjectId", request))%></font></td>
      </tr>
      <tr>
        <th></th>
        <td>
          <input type="submit" name="operation" class="btn btn-primary" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(FacultyAllocationCtl.OP_SAVE)%>">
          <input type="submit" name="operation" class="btn btn-secondary" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(FacultyAllocationCtl.OP_CANCEL)%>">
        </td>
      </tr>
    </table>
  </form>
</div>
