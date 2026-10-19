<%@ taglib uri="http://www.sunilos.com/ors-tags" prefix="ors" %>
<%@page import="com.sunilos.p4.ctl.MarksheetListCtl"%>
<%@page import="com.sunilos.p4.ctl.BaseCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="com.sunilos.p4.bean.MarksheetBean"%>
<%@page import="com.sunilos.p4.bean.CourseBean"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Iterator"%>

<%
    int pageNo   = ServletUtility.getPageNo(request);
    int pageSize = ServletUtility.getPageSize(request);
    int index    = ((pageNo - 1) * pageSize) + 1;
    List list    = ServletUtility.getList(request);
    Iterator<MarksheetBean> it = list.iterator();
    String _err  = ServletUtility.getErrorMessage(request);
    List<CourseBean> courseList = (List<CourseBean>) request.getAttribute("courseList");
    String selectedCourseId = ServletUtility.getParameter("courseId", request);
%>

<div class="container-fluid px-4 py-4" style="max-width:1100px;">
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden">

    <div class="card-header text-white border-0 py-3 px-4 d-flex justify-content-between align-items-center"
         style="background:linear-gradient(135deg,#0d2137 0%,#1565c0 100%);">
      <h5 class="mb-0 fw-bold"><i class="bi bi-file-earmark-text-fill me-2"></i> Course-Wise Marksheet List</h5>
      <div class="d-flex gap-2">
        <a href="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.MARKSHEET_REPORT_CTL)%>" target="_blank" class="btn btn-sm btn-warning fw-semibold">
          <i class="bi bi-file-earmark-pdf me-1"></i> Print PDF
        </a>
        <a href="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.MARKSHEET_REPORT_CTL)%>?type=doc" target="_blank" class="btn btn-sm btn-info fw-semibold">
          <i class="bi bi-file-earmark-word me-1"></i> Print DOC
        </a>
        <a href="MarksheetCtl" class="btn btn-sm btn-light text-primary fw-semibold">
          <i class="bi bi-plus-circle me-1"></i> <ors:message key="marksheet.add"/>
        </a>
      </div>
    </div>

    <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.MARKSHEET_LIST_CTL)%>" method="POST">
    <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
      <input type="hidden" name="pageNo"   value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(pageNo)%>">
      <input type="hidden" name="pageSize" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(pageSize)%>">

      <div class="p-3 bg-light border-bottom d-flex flex-wrap gap-2 align-items-center">
        <input type="text" name="name" class="form-control form-control-sm" style="max-width:180px;"
               placeholder="Student Name (Letters)" pattern="[A-Za-z]+"
               title="Only letters are allowed"
               oninput="this.value=this.value.replace(/[^A-Za-z]/g,'')"
               value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getParameter("name", request))%>">
        <input type="text" name="rollNo" class="form-control form-control-sm" style="max-width:140px;"
               placeholder="Roll No" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getParameter("rollNo", request))%>">
        <select name="courseId" class="form-select form-select-sm" style="max-width:170px;">
          <option value="">-- All Courses --</option>
          <% if (courseList != null) {
               for (CourseBean c : courseList) { %>
          <option value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getId())%>" <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.valueOf(c.getId()).equals(selectedCourseId) ? "selected" : "")%>><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getName())%></option>
          <%   }
             } %>
        </select>
        <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(BaseCtl.OP_SEARCH)%>" class="btn btn-primary btn-sm">
          <i class="bi bi-search me-1"></i> <ors:message key="button.search"/>
        </button>
        <% if ("Admin".equalsIgnoreCase((String)session.getAttribute("role"))) { %>
<button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(BaseCtl.OP_DELETE)%>" class="btn btn-danger btn-sm ms-auto"
                onclick="return confirm('Delete selected marksheets?')">
          <i class="bi bi-trash me-1"></i> <ors:message key="button.delete.selected"/>
        </button>
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
              <th><ors:message key="marksheet.rollno"/></th>
              <th><ors:message key="marksheet.student.name"/></th>
              <th>Course</th>
              <th>Subject</th>
              <th class="text-center">Marks (Out of 100)</th>
              <th class="text-center">Result</th>
              <th><ors:message key="label.action"/></th>
            </tr>
          </thead>
          <tbody>
            <% while(it.hasNext()) {
                 MarksheetBean bean = it.next();
                 int score = bean.getMarks() != null ? bean.getMarks() : 0;
                 boolean passed = score >= 35;
            %>
            <tr>
              <td><input type="checkbox" name="ids" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>"></td>
              <td class="text-muted small"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(index++)%></td>
              <td class="text-muted small"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%></td>
              <td class="fw-semibold"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getRollNo()))%></td>
              <td><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getName()))%></td>
              <td><span class="badge bg-primary"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getCourseName()))%></span></td>
              <td><span class="badge bg-info text-dark"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getSubjectName()))%></span></td>
              <td class="text-center fw-bold"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(score)%> / 100</td>
              <td class="text-center">
                <span class="badge <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(passed ? "bg-success" : "bg-danger")%>"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(passed ? "PASS" : "FAIL")%></span>
              </td>
              <td>
                <a href="MarksheetCtl?id=<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>" class="btn btn-sm btn-outline-primary">
                  <i class="bi bi-pencil"></i> <ors:message key="button.edit"/>
                </a>
              </td>
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
