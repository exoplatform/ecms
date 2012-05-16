================================================================================
  Copyright (C) 2003-2012 eXo Platform SAS.
 
  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU Affero General Public License
  as published by the Free Software Foundation; either version 3
  of the License, or (at your option) any later version.
 
  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this program; if not, see<http://www.gnu.org/licenses/>. 
================================================================================

TABLE OF CONTENTS
----------------------------------------------------
1. What is eXo Content
2. What's new?
3. Building from sources
4. Running


1. WHAT IS EXO CONTENT
----------------------------------------------------
	Enterprise Content Management is the strategies, methods and tools used to capture, manage, store, preserve and deliver 
contents and documents related to organizational processes with the purpose of improving operational productivity and efficiency. 
eXo Content tools and strategies allow the management of an organization's unstructured information, whether that information exists. 
The eXo Content portlet provides you with a portal solution that can help you achieve these processes easily and it is carefully 
designed so that you can leverage your business content across all formats for competitive gain. An environment for employees is also 
provided to share and collaborate digital contents as well as delivering a comprehensive unified solution with rich functionalities.
eXo Content consists of three parts: 
	DMS (Document Management System): used to store, manage and track electronic documents and electronic images. DMS allows 
documents to be modified and managed easily and conveniently by managing versions, properties, ect.
	WCM (Web Content Management): helps in maintaining, controlling, changing and reassembling the content on a web-page. It also 
helps webmasters who handle all tasks needed to run a website, including development, deployment, design, content publication and 
monitoring.				

2. WHAT's NEW?
---------------------------------------------------
	* Platform 4 is coming soon
	
3. BUILDING FROM SOURCES	
		This will explain you how to build a package of eXo Content with Tomcat.

		***********************
		* CHECK OUT PROJECT
		***********************
        * Clone project from github git@github.com:exodev/ecms.git
        
		*****************
		* COMPILATION
		*****************
        * Goto ecms folder
		* mvn clean install

		***********************
		* PACKAGING FOR TOMCAT:
		***********************
		
		*  A Tomcat instance will be created in /delivery/wcm/assembly/target/tomcat/ 
		
		*****************
		* STARTING:
		*****************		
		* On Tomcat(Mac/Linux): go to the tomcat directory and execute 'bin/gatein.sh run' ('bin/gatein.bat run' on Windows)
	
4. RUNNING
---------------------------------------------------
4.1 Open your web browsers, now eXo Content can run on FireFox 3.6 or newer, Internet Explorer 7 or newer (we recommend using FireFox 3+ or Internet Explorer 7+ for the best result)
		Navigate to URL: http://localhost:8080/ecmdemo

4.2 When the page has been loaded, click "Login" on the top right corner. Specify the username "root" and the password "gtn".


** Other resources and links
	Company site        http://www.exoplatform.com
	Community JIRA      http://jira.exoplatform.org
	Community site      http://community.exoplatform.org
	
For more documentation and latest updated news, please visit our websites:
	www.exoplatform.com
	blog.exoplatform.org
	
If you have questions, please send a mail to the list exo-ecms@exoplatform.com.

Thanks for using eXo Content product!
