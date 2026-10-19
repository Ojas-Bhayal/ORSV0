<%@ taglib uri="http://www.sunilos.com/ors-tags" prefix="ors" %>
<%@page import="com.sunilos.p4.ctl.BaseCtl"%>
<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.bean.CourseBean"%>
<%@page import="com.sunilos.p4.bean.SubjectBean"%>
<%@page import="com.sunilos.p4.util.DataUtility"%>
<%@page import="com.sunilos.p4.util.ServletUtility"%>
<%@page import="com.sunilos.p4.util.HTMLUtility"%>
<%@page import="java.util.List"%>

<jsp:useBean id="bean" class="com.sunilos.p4.bean.FacultyBean" scope="request"></jsp:useBean>

<%
    List collegeList = (List) request.getAttribute("collegeList");
    List<CourseBean> courseList = (List<CourseBean>) request.getAttribute("courseList");
    List<SubjectBean> subjectList = (List<SubjectBean>) request.getAttribute("subjectList");
    long selectedCourseId = bean.getCourseId() > 0 ? bean.getCourseId() : 1L;
    long selectedSubjectId = bean.getSubjectId() > 0 ? bean.getSubjectId() : 1L;
%>

<div class="container py-4" style="max-width:720px;">
  <div class="card border-0 shadow-sm rounded-4 overflow-hidden">

    <div class="card-header text-white border-0 py-3 px-4"
         style="background:linear-gradient(135deg,#0d2137 0%,#1565c0 100%);">
      <h5 class="mb-0 fw-bold">
        <i class="bi bi-person-badge-fill me-2"></i>
        <%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId() > 0 ? "Edit Faculty" : "Add Faculty")%>
      </h5>
    </div>

    <div class="card-body px-4 py-4">

      <ors:formMsg />

      <form name="facultyForm" action="FacultyCtl" method="POST">
<input type="hidden" name="csrfToken" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(session.getAttribute("csrfToken") != null ? session.getAttribute("csrfToken") : "")%>">
        <input type="hidden" name="id"               value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getId())%>">
        <input type="hidden" name="createdBy"         value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getCreatedBy())%>">
        <input type="hidden" name="modifiedBy"        value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(bean.getModifiedBy())%>">
        <input type="hidden" name="createdDatetime"   value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getTimestamp(bean.getCreatedDatetime()))%>">
        <input type="hidden" name="modifiedDatetime"  value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getTimestamp(bean.getModifiedDatetime()))%>">

        <!-- College, Course & Subject Allocation -->
        <div class="row g-3 mb-3">
          <div class="col-md-4">
            <label class="form-label fw-semibold">College <span class="text-danger">*</span></label>
            <%=HTMLUtility.getList("collegeId", String.valueOf(bean.getCollegeId()), collegeList)%>
            <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("collegeId", request))%></div>
          </div>
          <div class="col-md-4">
            <label class="form-label fw-semibold">Assigned Course <span class="text-danger">*</span></label>
            <select name="courseId" class="form-select">
              <% if (courseList != null && !courseList.isEmpty()) {
                   for (CourseBean c : courseList) { %>
              <option value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getId())%>" <%=com.sunilos.p4.util.HTMLUtility.escapeHtml((c.getId() == selectedCourseId) ? "selected" : "")%>><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(c.getName())%></option>
              <%   }
                 } else { %>
              <option value="">-- Select Course --</option>
              <% } %>
            </select>
          </div>
          <div class="col-md-4">
            <label class="form-label fw-semibold">Assigned Subject <span class="text-danger">*</span></label>
            <select name="subjectId" class="form-select">
              <% if (subjectList != null && !subjectList.isEmpty()) {
                   for (SubjectBean s : subjectList) { %>
              <option value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getId())%>" <%=com.sunilos.p4.util.HTMLUtility.escapeHtml((s.getId() == selectedSubjectId) ? "selected" : "")%>><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(s.getName())%></option>
              <%   }
                 } else { %>
              <option value="">-- Select Subject --</option>
              <% } %>
            </select>
          </div>
        </div>

        <!-- Name row (Strict letters only - Requirement 1) -->
        <div class="row g-3 mb-3">
          <div class="col-md-6">
            <label class="form-label fw-semibold">First Name <span class="text-danger">*</span></label>
            <input type="text" name="firstName" class="form-control"
                   placeholder="Letters only"
                   pattern="[A-Za-z]+"
                   title="Only letters are allowed"
                   oninput="this.value=this.value.replace(/[^A-Za-z]/g,'')"
                   value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getFirstName()))%>">
            <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("firstName", request))%></div>
          </div>
          <div class="col-md-6">
            <label class="form-label fw-semibold">Last Name <span class="text-danger">*</span></label>
            <input type="text" name="lastName" class="form-control"
                   placeholder="Letters only"
                   pattern="[A-Za-z]+"
                   title="Only letters are allowed"
                   oninput="this.value=this.value.replace(/[^A-Za-z]/g,'')"
                   value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getLastName()))%>">
            <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("lastName", request))%></div>
          </div>
        </div>

        <!-- Email (Requirement 5) -->
        <div class="mb-3">
          <label class="form-label fw-semibold">Email ID <span class="text-danger">*</span></label>
          <input type="email" name="email" class="form-control"
                 placeholder="example@domain.com"
                 value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getEmail()))%>">
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("email", request))%></div>
        </div>

        <!-- Mobile (Requirement 3: 10 digits starting with 6,7,8,9) -->
        <div class="mb-3">
          <label class="form-label fw-semibold">Mobile No <span class="text-danger">*</span></label>
          <input type="text" name="mobileNo" maxlength="10" pattern="[6-9][0-9]{9}"
                 title="10 digits starting with 6, 7, 8, or 9"
                 placeholder="10-digit mobile number starting with 6-9"
                 oninput="this.value=this.value.replace(/[^0-9]/g,'').slice(0,10)"
                 class="form-control"
                 value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getMobileNo()))%>">
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("mobileNo", request))%></div>
        </div>

        <!-- Address -->
        <div class="mb-3">
          <label class="form-label fw-semibold">Address <span class="text-danger">*</span></label>
          <textarea name="address" class="form-control" rows="2"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getStringData(bean.getAddress()))%></textarea>
          <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("address", request))%></div>
        </div>

        <!-- Gender + DOB row -->
        <div class="row g-3 mb-4">
          <div class="col-md-4">
            <label class="form-label fw-semibold">Gender</label>
            <select name="gender" class="form-select">
              <option value="">-- Select --</option>
              <option value="Male"   <%=com.sunilos.p4.util.HTMLUtility.escapeHtml("Male".equals(bean.getGender())   ? "selected" : "")%>>Male</option>
              <option value="Female" <%=com.sunilos.p4.util.HTMLUtility.escapeHtml("Female".equals(bean.getGender()) ? "selected" : "")%>>Female</option>
              <option value="Other"  <%=com.sunilos.p4.util.HTMLUtility.escapeHtml("Other".equals(bean.getGender())  ? "selected" : "")%>>Other</option>
            </select>
          </div>
          <div class="col-md-8">
            <label class="form-label fw-semibold">Date of Birth (MM/dd/yyyy) <span class="text-danger">*</span></label>
            <div class="input-group">
              <input type="text" name="dob" class="form-control" readonly
                     value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(DataUtility.getDateString(bean.getDob()))%>">
              <a class="btn btn-outline-secondary" href="javascript:void(0)" onclick="getCalendar(this.parentNode.querySelector('[name=dob]'));">
                <img src="../img/cal.jpg" width="16" height="15" alt="Calendar">
              </a>
            </div>
            <div class="text-danger small mt-1"><%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ServletUtility.getErrorMessage("dob", request))%></div>
          </div>
        </div>

        <!-- Buttons -->
        <div class="d-flex gap-2 pt-2 border-top">
          <button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(BaseCtl.OP_SAVE)%>" class="btn btn-primary">
            <i class="bi bi-save me-1"></i> Save
          </button>
          <% if(bean.getId() > 0) { %>
          <% if ("Admin".equalsIgnoreCase((String)session.getAttribute("role"))) { %>
<button type="submit" name="operation" value="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(BaseCtl.OP_DELETE)%>" class="btn btn-danger"
                  onclick="return confirm('Delete this faculty member?')">
            <i class="bi bi-trash me-1"></i> Delete
          </button>
<% } %>
          <% } %>
          <a href="<%=com.sunilos.p4.util.HTMLUtility.escapeHtml(ORSView.FACULTY_LIST_CTL)%>" class="btn btn-secondary ms-auto">
            <i class="bi bi-x-circle me-1"></i> Cancel
          </a>
        </div>
      </form>
    </div>
  </div>
</div>
