<%@page import="com.sunilos.p4.ctl.FacultyAllocationListCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.bean.FacultyAllocationBean"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>
<div class="container py-4">
  <h3>Faculty Teaching Allocations</h3>
  <H4><font color="red"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage(request))%></font></H4>
  <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.FACULTY_ALLOCATION_LIST_CTL)%>" method="post">
  <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
    <%
        int pageNo = ServletUtility.getPageNo(request);
        int pageSize = ServletUtility.getPageSize(request);
        List list = ServletUtility.getList(request);
        Iterator<FacultyAllocationBean> it = list.iterator();
    %>
    <table class="table table-striped table-bordered mt-3">
      <thead>
        <tr>
          <th>Select</th>
          <th>Faculty Name</th>
          <th>Faculty Email</th>
          <th>Course</th>
          <th>Subject</th>
          <th>Edit</th>
        </tr>
      </thead>
      <tbody>
        <% while (it.hasNext()) {
             FacultyAllocationBean bean = it.next(); %>
        <tr>
          <td><input type="checkbox" name="ids" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>"></td>
          <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getFacultyName()))%></td>
          <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getFacultyEmail()))%></td>
          <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getCourseName()))%></td>
          <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getSubjectName()))%></td>
          <td><a href="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.FACULTY_ALLOCATION_CTL)%>?id=<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>">Edit</a></td>
        </tr>
        <% } %>
      </tbody>
    </table>
    <input type="hidden" name="pageNo" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(pageNo)%>">
    <input type="hidden" name="pageSize" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(pageSize)%>">
  </form>
</div>
