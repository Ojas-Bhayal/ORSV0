<%@page import="com.sunilos.p4.ctl.EnrollmentListCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.bean.EnrollmentBean"%>
<%@page import="com.sunilos.p4.bean.CourseBean"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>
<div class="container py-4">
  <h3>Student Course Enrollments</h3>
  <H4><font color="red"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage(request))%></font></H4>
  <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.ENROLLMENT_LIST_CTL)%>" method="post">
  <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
    <%
        int pageNo = ServletUtility.getPageNo(request);
        int pageSize = ServletUtility.getPageSize(request);
        List list = ServletUtility.getList(request);
        Iterator<EnrollmentBean> it = list.iterator();
    %>
    <table class="table table-striped table-bordered mt-3">
      <thead>
        <tr>
          <th>Select</th>
          <th>Roll No</th>
          <th>Student Name</th>
          <th>Course</th>
          <th>Semester</th>
          <th>Status</th>
          <th>Edit</th>
        </tr>
      </thead>
      <tbody>
        <% while (it.hasNext()) {
             EnrollmentBean bean = it.next(); %>
        <tr>
          <td><input type="checkbox" name="ids" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>"></td>
          <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getRollNo()))%></td>
          <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getStudentName()))%></td>
          <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getCourseName()))%></td>
          <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getSemester()))%></td>
          <td><span class="badge <%=com.sunilos.p4.util.HTMLUtility.escapeHtml("ACTIVE".equalsIgnoreCase(bean.getStatus()) ? "bg-success" : "bg-secondary")%>"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getStatus()))%></span></td>
          <td><a href="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.ENROLLMENT_CTL)%>?id=<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>">Edit</a></td>
        </tr>
        <% } %>
      </tbody>
    </table>
    <input type="hidden" name="pageNo" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(pageNo)%>">
    <input type="hidden" name="pageSize" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(pageSize)%>">
  </form>
</div>
