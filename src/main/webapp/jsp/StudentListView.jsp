<%@page import="com.sunilos.p4.ctl.StudentListCtl"%>
<%@page import="com.sunilos.p4.ctl.BaseCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="com.sunilos.p4.bean.StudentBean"%>
<%@page import="com.sunilos.p4.bean.CourseBean"%>
<%@page import="com.sunilos.p4.bean.UserBean"%>
<%@page import="com.sunilos.p4.bean.RoleBean"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>

<%
    int pageNo   = ServletUtility.getPageNo(request);
    int pageSize = ServletUtility.getPageSize(request);
    int index    = ((pageNo - 1) * pageSize) + 1;
    List list    = ServletUtility.getList(request);
    Iterator<StudentBean> it = list.iterator();
    String _err  = ServletUtility.getErrorMessage(request);
    List<CourseBean> courseList = (List<CourseBean>) request.getAttribute("courseList");
    String selectedCourseId = ServletUtility.getParameter("courseId", request);
    UserBean currentUser = (UserBean) session.getAttribute("user");
    boolean isAdmin = currentUser != null && currentUser.getRoleId() == RoleBean.ADMIN;
%>

<div class="container-fluid px-4 py-4" style="max-width:1150px;">
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden">

    <div class="card-header text-white border-0 py-3 px-4 d-flex justify-content-between align-items-center"
         style="background:linear-gradient(135deg,#0d2137 0%,#1565c0 100%);">
      <h5 class="mb-0 fw-bold"><i class="bi bi-mortarboard-fill me-2"></i> Student &amp; Course Roster List</h5>
      <div class="d-flex gap-2">
        <a href="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.STUDENT_REPORT_CTL)%>" target="_blank" class="btn btn-sm btn-warning fw-semibold">
          <i class="bi bi-file-earmark-pdf me-1"></i> Print PDF
        </a>
        <a href="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.STUDENT_REPORT_CTL)%>?type=doc" target="_blank" class="btn btn-sm btn-info fw-semibold">
          <i class="bi bi-file-earmark-word me-1"></i> Print DOC
        </a>
        <% if (isAdmin) { %>
        <a href="StudentCtl" class="btn btn-sm btn-light text-primary fw-semibold">
          <i class="bi bi-plus-circle me-1"></i> Add Student
        </a>
        <% } %>
      </div>
    </div>

    <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.STUDENT_LIST_CTL)%>" method="POST">
    <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
      <input type="hidden" name="pageNo"   value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(pageNo)%>">
      <input type="hidden" name="pageSize" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(pageSize)%>">

      <div class="p-3 bg-light border-bottom d-flex flex-wrap gap-2 align-items-center">
        <input type="text" name="firstName" class="form-control form-control-sm" style="max-width:150px;"
               placeholder="First Name (Letters)" pattern="[A-Za-z]+"
               title="Only letters are allowed"
               oninput="this.value=this.value.replace(/[^A-Za-z]/g,'')"
               value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getParameter("firstName", request))%>">
        <input type="text" name="lastName" class="form-control form-control-sm" style="max-width:150px;"
               placeholder="Last Name (Letters)" pattern="[A-Za-z]+"
               title="Only letters are allowed"
               oninput="this.value=this.value.replace(/[^A-Za-z]/g,'')"
               value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getParameter("lastName", request))%>">
        <input type="email" name="email" class="form-control form-control-sm" style="max-width:190px;"
               placeholder="Email ID" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getParameter("email", request))%>">
        <select name="courseId" class="form-select form-select-sm" style="max-width:160px;">
          <option value="">-- All Courses --</option>
          <% if (courseList != null) {
               for (CourseBean c : courseList) { %>
          <option value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getId())%>" <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.valueOf(c.getId()).equals(selectedCourseId) ? "selected" : "")%>><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getName())%></option>
          <%   }
             } %>
        </select>
        <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(BaseCtl.OP_SEARCH)%>" class="btn btn-primary btn-sm">
          <i class="bi bi-search me-1"></i> Search
        </button>
        <% if (isAdmin) { %>
        <% if ("Admin".equalsIgnoreCase((String)session.getAttribute("role"))) { %>
<button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(BaseCtl.OP_DELETE)%>" class="btn btn-danger btn-sm ms-auto"
                onclick="return confirm('Delete selected students?')">
          <i class="bi bi-trash me-1"></i> Delete Selected
        </button>
<% } %>
        <% } %>
      </div>

      <% if(_err != null && !_err.isEmpty()) { %>
      <div class="alert alert-danger py-2 mx-3 mt-3"><i class="bi bi-exclamation-triangle-fill me-2"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(_err)%></div>
      <% } %>

      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th width="40"><input type="checkbox" onclick="document.querySelectorAll('input[name=ids]').forEach(c=>c.checked=this.checked)"></th>
              <th>#</th>
              <th>ID</th>
              <th>College</th>
              <th>Course</th>
              <th>First Name</th>
              <th>Last Name</th>
              <th>Date of Birth</th>
              <th>Mobile No</th>
              <th>Email ID</th>
              <% if (isAdmin) { %><th>Action</th><% } %>
            </tr>
          </thead>
          <tbody>
            <% while(it.hasNext()) { StudentBean bean = it.next(); %>
            <tr>
              <td><input type="checkbox" name="ids" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>"></td>
              <td class="text-muted small"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(index++)%></td>
              <td class="text-muted small"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%></td>
              <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getCollegeName() != null ? bean.getCollegeName() : bean.getCollegeId())%></td>
              <td><span class="badge bg-primary"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getCourseName() != null ? bean.getCourseName() : "")%></span></td>
              <td class="fw-semibold"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getFirstName())%></td>
              <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getLastName())%></td>
              <td class="text-muted small"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getDob())%></td>
              <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getMobileNo())%></td>
              <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getEmail())%></td>
              <% if (isAdmin) { %>
              <td>
                <a href="StudentCtl?id=<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>" class="btn btn-sm btn-outline-primary">
                  <i class="bi bi-pencil"></i> Edit
                </a>
              </td>
              <% } %>
            </tr>
            <% } %>
          </tbody>
        </table>
      </div>

      <div class="p-3 border-top">
        <%@ include file="ListFooter.jsp"%>
      </div>
    </form>
  </div>
</div>
