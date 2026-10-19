<%@ taglib uri="http://www.sunilos.com/ors-tags" prefix="ors" %>
<%@page import="com.sunilos.p4.ctl.MarksheetMeritListCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="com.sunilos.p4.bean.MarksheetBean"%>
<%@page import="com.sunilos.p4.bean.CourseBean"%>
<%@page import="com.sunilos.p4.bean.SubjectBean"%>
<%@page import="com.sunilos.p4.bean.UserBean"%>
<%@page import="com.sunilos.p4.bean.RoleBean"%>
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
    List<SubjectBean> subjectList = (List<SubjectBean>) request.getAttribute("subjectList");
    Object selCourseAttr = request.getAttribute("selectedCourseId");
    Object selSubjectAttr = request.getAttribute("selectedSubjectId");
    String selectedCourseId = selCourseAttr != null ? String.valueOf(selCourseAttr) : ServletUtility.getParameter("courseId", request);
    String selectedSubjectId = selSubjectAttr != null ? String.valueOf(selSubjectAttr) : ServletUtility.getParameter("subjectId", request);
    Double classAvg = (Double) request.getAttribute("classAverage");

    UserBean currentUser = (UserBean) session.getAttribute("user");
    boolean anonymizePeers = (currentUser != null && currentUser.getRoleId() == RoleBean.STUDENT);
%>

<div class="container-fluid px-4 py-4" style="max-width:1050px;">
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden">

    <div class="card-header text-white border-0 py-3 px-4 d-flex justify-content-between align-items-center"
         style="background:linear-gradient(135deg,#0d2137 0%,#1565c0 100%);">
      <div>
        <h5 class="mb-0 fw-bold"><i class="bi bi-trophy-fill me-2"></i> Subject-Specific Merit List &amp; Class Benchmarks</h5>
        <% if (anonymizePeers) { %>
        <small class="opacity-75"><i class="bi bi-shield-lock me-1"></i> Peer identities are hidden for privacy.</small>
        <% } %>
      </div>
      <% if (classAvg != null && classAvg > 0) { %>
      <span class="badge bg-warning text-dark px-3 py-2 fw-bold">
        Subject Class Average: <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.format("%.2f%%", classAvg))%>
      </span>
      <% } %>
    </div>

    <form action="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.MARKSHEET_MERIT_LIST_CTL)%>" method="POST">
    <input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">

      <!-- Course & Subject Filter Bar -->
      <div class="p-3 bg-light border-bottom d-flex flex-wrap gap-2 align-items-center">
        <label class="fw-semibold small text-secondary me-1">Course:</label>
        <select name="courseId" class="form-select form-select-sm" style="max-width:200px;" onchange="this.form.submit()">
          <% if (courseList != null) {
               for (CourseBean c : courseList) { %>
          <option value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getId())%>" <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.valueOf(c.getId()).equals(selectedCourseId) ? "selected" : "")%>><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getName())%></option>
          <%   }
             } %>
        </select>

        <label class="fw-semibold small text-secondary ms-2 me-1">Subject:</label>
        <select name="subjectId" class="form-select form-select-sm" style="max-width:220px;" onchange="this.form.submit()">
          <% if (subjectList != null) {
               for (SubjectBean s : subjectList) {
                 if (selectedCourseId == null || selectedCourseId.isEmpty() || String.valueOf(s.getCourseId()).equals(selectedCourseId)) { %>
          <option value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getId())%>" <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.valueOf(s.getId()).equals(selectedSubjectId) ? "selected" : "")%>><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getName())%></option>
          <%     }
               }
             } %>
        </select>

        <button type="submit" class="btn btn-primary btn-sm">
          <i class="bi bi-funnel me-1"></i> Filter Subject Ranking
        </button>
      </div>

      <% if(_err != null && !_err.isEmpty()) { %>
      <div class="alert alert-danger py-2 mx-3 mt-3"><i class="bi bi-exclamation-triangle-fill me-2"></i><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(_err)%></div>
      <% } %>

      <div class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th width="70" class="text-center"><ors:message key="marksheet.rank"/></th>
              <th><ors:message key="marksheet.rollno"/></th>
              <th><ors:message key="marksheet.student.name"/></th>
              <th>Course</th>
              <th>Subject</th>
              <th class="text-center">Marks (Out of 100)</th>
              <th class="text-center">Percentage</th>
              <th class="text-center">Status</th>
            </tr>
          </thead>
          <tbody>
            <% int rank = index;
               while(it.hasNext()) {
                 MarksheetBean bean = it.next();
                 String displayRoll = DataUtility.getStringData(bean.getRollNo());
                 String displayName = DataUtility.getStringData(bean.getName());
                 boolean isSelf = anonymizePeers && !MarksheetMeritListCtl.ANONYMOUS_PEER_NAME.equals(displayName);

                 int score = bean.getMarks() != null ? bean.getMarks() : 0;
                 double pct = bean.getPercentage();
                 boolean passed = score >= 35;
            %>
            <tr class="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(isSelf ? "table-info" : "")%>">
              <td class="text-center">
                <% if(rank == 1) { %>
                  <span class="badge bg-warning text-dark fs-6"><i class="bi bi-trophy-fill"></i> <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(rank++)%></span>
                <% } else if(rank == 2) { %>
                  <span class="badge bg-secondary fs-6"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(rank++)%></span>
                <% } else if(rank == 3) { %>
                  <span class="badge" style="background:#cd7f32;font-size:1rem;"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(rank++)%></span>
                <% } else { %>
                  <span class="text-muted"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(rank++)%></span>
                <% } %>
              </td>
              <td class="fw-semibold"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(displayRoll)%></td>
              <td>
                <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(displayName)%>
                <% if (isSelf) { %><span class="badge bg-primary ms-1">You</span><% } %>
              </td>
              <td><span class="badge bg-primary"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getCourseName()))%></span></td>
              <td><span class="badge bg-info text-dark"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getSubjectName()))%></span></td>
              <td class="text-center"><span class="badge bg-dark rounded-pill px-3"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(score)%> / 100</span></td>
              <td class="text-center"><span class="badge bg-primary rounded-pill px-3"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(String.format("%.2f%%", pct))%></span></td>
              <td class="text-center"><span class="badge <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(passed ? "bg-success" : "bg-danger")%>"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(passed ? "PASS" : "FAIL")%></span></td>
            </tr>
            <% } %>
          </tbody>
        </table>
      </div>

      <div class="p-3 border-top d-flex justify-content-end">
        <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(MarksheetMeritListCtl.OP_BACK)%>"
                class="btn btn-outline-secondary">
          <i class="bi bi-arrow-left me-1"></i> <ors:message key="button.back"/>
        </button>
      </div>
    </form>
  </div>
</div>
