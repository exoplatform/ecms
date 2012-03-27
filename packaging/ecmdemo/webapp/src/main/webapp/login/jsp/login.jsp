<%--

    Copyright (C) 2009 eXo Platform SAS.
    
    This is free software; you can redistribute it and/or modify it
    under the terms of the GNU Lesser General Public License as
    published by the Free Software Foundation; either version 2.1 of
    the License, or (at your option) any later version.
    
    This software is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
    Lesser General Public License for more details.
    
    You should have received a copy of the GNU Lesser General Public
    License along with this software; if not, write to the Free
    Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
    02110-1301 USA, or see the FSF site: http://www.fsf.org.

--%>

<%@ page import="java.net.URLEncoder"%>
<%@ page import="javax.servlet.http.Cookie"%>
<%@ page import="org.exoplatform.container.PortalContainer"%>
<%@ page import="org.exoplatform.services.resources.ResourceBundleService"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="org.exoplatform.web.login.InitiateLoginServlet"%>
<%@ page import="org.gatein.common.text.EntityEncoder"%>
<%@ page language="java" %>
<%
 
  String contextPath = request.getContextPath() ;

  String username = request.getParameter("j_username");
  if(username == null) username = "";
 	String password = request.getParameter("j_password");
 	if(password == null) password = "";

  ResourceBundleService service = (ResourceBundleService) PortalContainer.getCurrentInstance(session.getServletContext())
  														.getComponentInstanceOfType(ResourceBundleService.class);
  ResourceBundle res = service.getResourceBundle(service.getSharedResourceBundleNames(), request.getLocale()) ;
  
  String lang = request.getLocale().getLanguage();
  
  Cookie cookie = new Cookie(InitiateLoginServlet.COOKIE_NAME, "");
	cookie.setPath(request.getContextPath());
	cookie.setMaxAge(0);
	response.addCookie(cookie);

  String uri = (String)request.getAttribute("org.gatein.portal.login.initial_uri");

  response.setCharacterEncoding("UTF-8"); 
  response.setContentType("text/html; charset=UTF-8");
%>
<!DOCTYPE html 
    PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
           "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="<%=lang%>" lang="<%=lang%>">
  <head>
    <title>Login</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>   
    <link rel="shortcut icon" type="image/x-icon"  href="<%=contextPath%>/favicon.ico" />
    <link rel='stylesheet' type='text/css' href='<%=contextPath%>/login/skin/Stylesheet.css'/>
    <script type="text/javascript" src="/eXoResources/javascript/eXo.js"></script>
    <script type="text/javascript" src="/eXoResources/javascript/eXo/portal/UIPortalControl.js"></script>
  </head>
  <body>
    <div class="UILogin">
      <div class="LoginHeader"></div>
      <div class="LoginContent">
				<div style="line-height: 12px; padding: 6px 3px 0 0; height: 27px; font-size: 11px;">
					<%/*Begin form*/%>
          <%
            if(username.length() > 0 || password.length() > 0) {
               EntityEncoder encoder = EntityEncoder.FULL;
               username = encoder.encode(username);

          %>
          <font color="red"><%=res.getString("UILoginForm.label.SigninFail")%></font><%}%>
				</div>
        <div class="CenterLoginContent">
          <form name="loginForm" action="<%= contextPath + "/login"%>" method="post" style="margin: 0px;">
                <% if (uri != null) { %>
          		<input type="hidden" name="initialURI" value="<%=uri%>"/>
                <% } %>
								
					<div class="FieldLabel"><label for="username"><%=res.getString("UILoginForm.label.UserName")%></label></div>
					<div>
            <input class="UserName" id="username" name="username" type="text" value="<%=username%>"/>
          </div>
				
					<div class="FieldLabel"><label for="password"><%=res.getString("UILoginForm.label.password")%></label></div>
					<div id="UIPortalLoginFormControl" onkeypress="eXo.portal.UIPortalControl.onEnterPress(event);">
            <input class="Password" type="password" id="password" name="password" value=""/>
          </div>  
					
					<div class="FieldLabel" onkeypress="eXo.portal.UIPortalControl.onEnterPress(event);">
						<input type="checkbox" class="checkbox" id="rememberme" name="rememberme" value="true"/>
						<label for="rememberme"><%=res.getString("UILoginForm.label.RememberOnComputer")%></label>
					</div>
		         
					<div id="UIPortalLoginFormAction" class="LoginButton" onclick="login();">
						<div class="LeftButton">
							<div class="RightButton">
								<div class="MiddleButton">
									<a href="#"><%=res.getString("UILoginForm.label.Signin")%></a>
								</div>
							</div>
						</div>  
					</div>
					<script type='text/javascript'>			            
					function login() {
						document.loginForm.submit();                   
					}
				</script>
				</form>
				<%/*End form*/%>
        </div>
      </div>
    </div>
    <div style="font-size: 11px; color: #3f3f3f; text-align: center">Copyright &copy; 2010 eXo Platform SAS, all rights reserved.</div>
  </body>
</html>
