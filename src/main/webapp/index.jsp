<%@page import="com.sunilos.p4.ctl.ORSView"%>
<%@page import="com.sunilos.p4.util.DataValidator"%>
<%@page import="com.sunilos.p4.util.MessageSource"%>
<%@page import="com.sunilos.p4.util.HTMLUtility"%>

<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="http://www.sunilos.com/ors-tags" prefix="ors" %>

<%
    String contextPath = request.getContextPath();
    String orsLang = String.valueOf(request.getAttribute("orsLanguage") == null ? "en" : request.getAttribute("orsLanguage"));
    if (!"hi".equals(orsLang) && !"en".equals(orsLang)) { orsLang = "en"; }
   %>
<html lang="<%= orsLang %>">
   <head>
      <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
      <meta name="viewport" content="width=device-width, initial-scale=1.0">
      <title>ORS &mdash; Online Result System</title>
      <link rel="icon" href="<%=HTMLUtility.escapeHtml(contextPath)%>/img/favicon.ico" type="image/x-icon">
      <!-- Bootstrap 5 -->
      <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css">
      <!-- Bootstrap Icons -->
      <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
      <!-- Font Awesome -->
      <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/5.14.0/css/all.min.css">
      <!-- Custom overrides -->
      <link rel="stylesheet" href="<%=HTMLUtility.escapeHtml(contextPath)%>/css/app.css">
      <script type="text/javascript" src="<%=HTMLUtility.escapeHtml(contextPath)%>/js/calendar.js"></script>
      <script src="${pageContext.request.contextPath}/js/ors-language.js"></script>
   </head>
   
   <body>
   
      <%@ include file="jsp/Header.jsp"%>
   
      <%
         // The included view must come from the server-side forwardPage attribute.
         // Never allow a request parameter to select an arbitrary JSP include.
         String viewName = (String) request.getAttribute("p");
         if (DataValidator.isNull(viewName)
                 || !viewName.startsWith("jsp/")
                 || viewName.contains("..")
                 || viewName.indexOf('\\') >= 0
                 || viewName.indexOf('\0') >= 0) {
             viewName = "jsp/404.jsp";
         }
         %>
      
      <div class="page-content">
         <jsp:include page="<%=HTMLUtility.escapeHtml(viewName)%>" />
      </div>
      
      <%@include file="jsp/Footer.jsp"%>
      <!-- Bootstrap 5 JS -->
      <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
      <script src="<%= contextPath %>/js/ors-language.js" defer></script>
   </body>
</html>