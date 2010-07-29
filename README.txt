================================================================================
  Copyright (C) 2003-2010 eXo Platform SAS.
 
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
---------------------------------------------------
1. What is eXo Content
2. What's new?
3. How to set up eXo Content
4. Building from sources
5. How to install WCM in JBoss/GateIn and EPP5
6. Running
7. Release notes


1. WHAT IS EXO CONTENT
---------------------------------------------------
	Enterprise Content Management is the strategies, methods and tools used to capture, manage, store, preserve and deliver 
contents and documents related to organizational processes with the purpose of improving operational productivity and efficiency. 
eXo Content tools and strategies allow the management of an organization's unstructured information, whether that information exists. 
The eXo Content portlet provides you with a portal solution that can help you achieve these processes easily and it is carefully 
designed so that you can leverage your business content across all formats for competitive gain. An environment for employees is also 
provided to share and collaborate digital contents as well as delivering a comprehensive unified solution with rich functionalities.
eXo Content consists of three parts: 
	DMS (Document Management System): used to store, manage and track electronic documents and electronic images. DMS allows 
documents to be modified and managed easily and conveniently by managing versions, properties, ect.
	Workflow: is the way of looking at and controlling the processes presented in an organization such as service provision or 
information processing, etc. It is an effective tool to use in times of crisis to make certain that the processes are efficient and 
effective with the purpose of better and more cost efficient organization.
	WCM (Web Content Management): helps in maintaining, controlling, changing and reassembling the content on a web-page. It also 
helps webmasters who handle all tasks needed to run a website, including development, deployment, design, content publication and 
monitoring.				

2. WHAT's NEW?
---------------------------------------------------
	* EAR compliance with EPP5
	*	CKEditor3 compatibility, Simple Json based UITable for dialogs/views
	*	Rest service for cleanName in nodes
	*	Authoring Plugin (as Tech Preview for RedHat)
	*	L11n (as Tech Preview for RedHat)
	* PDF View for almost document: MS Document, Openfice Document
	Find more details at the RELEASE NOTEs part

3. HOW TO SET UP EXO CONTENT
---------------------------------------------------
eXo Content requires the Java 2 Standard Edition Runtime
Environment (JRE) or Java Development Kit version 6.x

3.1 Install Java SE 1.6 (Java Development Kit)
    Based on JavaEE, our WCM runs currently fine with version 1.6 so if you are using newer version, please download and install this version to make WCM works fine. We will support newer version of Java very soon.

3.2 Download the lastest eXo Web Content Management version from: http://forge.objectweb.org/projects/exoplatform/

3.3 Unzip that package under a path that does not contain any space (in Windows).

3.4 Open a shell session and go to the bin/ directory that has just been extracted.

3.5 Then run the command :
		* Start tomcat server
			+)On the Windows platform
				Open a DOS prompt command, go to tomcat/bin and type the command:
					"gatein.bat run" for production
					"gatein-dev.bat run" for development 

			+)On Unix/Linux/cygwin
				Open a terminal, go to tomcat/bin and type the command:
					 "./gatein.sh run" for production
					 "./gatein-dev.sh run" for development
		
		* Start JBoss server
			+) On the Windows platform
				 Open a DOS prompt command, go to jboss/bin and type the command:
					"run.bat"

			+) On Unix/Linux/cygwin
				 Open a terminal, go to jboss/bin and type the command:
					 "./run.sh" for production
3.6 Run Openoffice service to convert document to PDF file for using PDF View.
		OpenOffice should available on your machine.
      * Run command:
		soffice -headless -accept="socket,host=127.0.0.1,port=8100;urp;" -nofirststartwizard
		Please refer to: http://www.artofsolving.com for using JODCONVERTER
3.7 Using PDF Viewer
		Now we using icepdf to enable viewing function in eXo Content
		Some PDF document make Icet pdf generating image with wrong font. We can improve this by
		installing some Ghostscripts font. Please refer here: http://wiki.icefaces.org/display/PDF/Optimized+Font+Substitution

4. BUILDING FROM SOURCES	
		This will explain you how to build a package of WCM 2.0.0-GA with Tomcat.
		For general information about installation, please refer to :
		http://anonsvn.jboss.org/repos/gatein/portal/trunk/README.txt

		*****************
		* COMPILATION
		*****************

		* mvn install
		For example: mvn install

		***********************
		* PACKAGING FOR TOMCAT:
		***********************
		WCM only:
		** mvn clean install in /delivery/wcm/assembly
		*  Creates a Tomcat delivery in /delivery/wcm/assembly/target/tomcat/ 
		
		WCM + WKF:
		** mvn clean install in /delivery/wkf-wcm/assembly
		*  Creates a Tomcat delivery in /delivery/wkf-wcm/assembly/target/tomcat/ 
		
		***************************
		* PACKAGING FOR JBOSS EARs:
		***************************		
		* WCM extension ear:
		** mvn clean install in /packaging/wcm/ear 
		  Get this file in target folder: gatein-wcm-extension-2.0.1.ear
		
		
		* ECMDEMO ear
		** mvn clean install in /packaging/ecmdemo/ear
		  Get this file in target folder: gatein-ecmdemo-porta-2.0.1.ear
		
		* Workflow extension ear(optional):
		** mvn clean install in /packaging/workflow/ear
		   Get this file in target folder: gatein-workflow-extension-2.0.1.ear

5. HOW TO INSTALL WCM IN JBoss/GateIn & EPP5

	5.1. Pre-requisites
	--------------------
	- Java 6
	- EPP5 zip
	- JBoss/GateIn zip

	5.2. Installation
	--------------
	+) For EPP5
	   * Unzip EPP5. Assume it's located at /working/jboss-epp-5.0
		   * Copy gatein-wcm-extension-2.0.1.ear and gatein-ecmdemo-portal-2.0.1.ear
	    /working/jboss-epp-5.0/jboss-as/server/default/deploy/
		 * Delete existing gatein-ds.xml and overwrite it with attached gatein-ds.xml and wcm-ds.xml
	
	+) For JBoss/GateIn
	   * Unzip JBoss/GateIn. Assum it's located at /working/jboss-gatein
	   * Copy gatein-wcm-extension-2.0.1.ear and gatein-ecmdemo-portal-2.0.1.ear
	    /working/jboss-gatein/server/default/deploy/
	
	5.3. Start up
	-------------
  +) On the Windows platform
     Open a DOS prompt command, go to /working/jboss-epp-5.0/jboss-as/bin and type the command:
		 "run.bat"
		 
  +) On Unix/Linux/cygwin
		 Open a terminal, go to /working/jboss-epp-5.0/jboss-as/bin and type the command:
		 "./run.sh" for production

6. RUNNING
---------------------------------------------------

	6.1 Running
	------------------------
	    Open your web browsers, now eXo Content can run on FireFox 2 or newer, Internet Explorer 7 or newer (we recommend using FireFox 3+ or Internet Explorer 7+ for the best result)
		  Navigate to URL: 
	    http://localhost:8080/ecmdemo/public/classic
	    http://localhost:8080/ecmdemo/public/acme

  6.2 Login
	------------------------
	   When the page has been loaded, click "Login" on the top right corner. 
		 Specify the username and the password.
		 ----------------------
		 Username  |   Password
       root    |     gtn
			 john    |     gtn
			 james   |     gtn
			 mary    |     gtn
			 demo    |     gtn

7. RELEASE NOTES 
---------------------------------------------------

Release Notes - exo-ecms - Version wcm-2.0.1

** Bug
    * [ECMS-183] - Can't add gadgets into  web content
    * [ECMS-206] - Duplicate membership * in Permission Popup
    * [ECMS-284] - link of document on folder which was add exo:taxonomy action is still displayed content when this document was deleted 
    * [ECMS-344] - In Content Selector form, Uploaded file is not saved 
    * [ECMS-357] - can not download file in ContentListView
    * [ECMS-398] - Issue with accent (French) in Edit content dialog in SCV portlet (encoding?)
    * [ECMS-466] - Can not show node content in SCV or CLV after export/import node (with wcm-publication-configuration.xml)
    * [ECMS-469] - Can not store a node having a mandatory multiple property
    * [ECMS-500] - Site's skin broken after save on Edit skin layout
    * [ECMS-509] - Application Error Message in Validator
    * [ECMS-510] -  Error when delete user from "User and group management"
    * [ECMS-584] - WCM: Can not download an uploaded file
    * [ECMS-648] - Impossible to download binary with name containing illegal jcr char using the right popup menu
    * [ECMS-650] - Error while setting a page with PCLV portlet
    * [ECMS-653] - Left menu in Site explorer displays wrongly name of uploaded file if the name contains special character like '
    * [ECMS-656] - occur exception when edit a View template
    * [ECMS-674] - exo:processRecord didn't added by default when create file plan
    * [ECMS-688] - Cannot delete group
    * [ECMS-758] - Add change state for OBSOLETE state in AuthoringPublicationPlugin
    * [ECMS-771] - Cannot access to Manage users from Newsletter Manager
    * [ECMS-772] - The category path is wrong when add a category to a document from "Manage categories" action
    * [ECMS-773] - After change state, the author always is _system. Should be current user
    * [ECMS-774] - Can not set an empty Date field after editing it once
    * [ECMS-775] - Cannot change permission of a content to set "read only permission
    * [ECMS-779] - Content explorer portlet breaks under "portal" container
    * [ECMS-781] - Resource bundle cannot load in publication plugins and authoring plugin
    * [ECMS-784] - Exception when open form to manage user in Newsletter Manage
    * [ECMS-804] - Update of a new document (file) does not change the publication status
    * [ECMS-811] - build WCM2 on jboss
    * [ECMS-812] - Content Publishing Process : ClassCastException
    * [ECMS-814] - Exception when select category of Classic/Events in Category portlet
    * [ECMS-817] - create a page, add a Form : Content Creator, the page will be blank in public mode
    * [ECMS-856] - Missing method delete in WebDavService (core-services)
    * [ECMS-875] - Can not switch  tabs in Multi Languages form
    * [ECMS-887] - WCM WebDAV service replaces file and content nodetype
    * [ECMS-888] - Perf issue on trunk
    * [ECMS-909] - Hardcoded and wrong rest name in ContentView.gtmpl
    * [ECMS-916] - Category field for selection does not show up in editnode tab (content-publishing process).
    * [ECMS-930] - A date field is shown instead of text arer field when property is multiple
    * [ECMS-1051] - After session timeout, site explorer become unusable when adding two actions
    * [ECMS-1056] - WebDAV access does not work on some platforms

** Documentation
    * [ECMS-961] - Document how to install WCM in JBoss/GateIn and EPP5

** Feedback
    * [ECMS-825] - Workspace name in content-publishing process
    * [ECMS-874] - Get the correct contanier in a job
    * [ECMS-891] - Acceptable MymeTypes & selectorParams for UIOneNodePathSelector  

** Task
    * [ECMS-755] - Create branches WCM 2.0.x
    * [ECMS-756] - Upgrade to use GateIn 3.1.0-CR01
    * [ECMS-770] - Update system properties for server patch
    * [ECMS-789] - Rollback to use UIFormMultiValueInputSet of GateIn
    * [ECMS-797] - Backport the fix for ordering symlinks
    * [ECMS-805] - Upgrade to Gatein 3.1
    * [ECMS-834] - Release WCM 2.0.1
    * [ECMS-858] - Upgrade to use JCR 1.12.2-CP01
    * [ECMS-868] - Test Campaign on 2.0.1
    * [ECMS-968] - EPP5 for WCM 2.0.1
    * [ECMS-1082] - Should have a good final name for ear files

** Sub-task
    * [ECMS-739] -  [WCM] - Error occurs when trying to do actions with category in Newsletter Manager


** Other resources and links
	Company site        http://www.exoplatform.com
	Community JIRA      http://jira.exoplatform.org
	Community site      http://www.exoplatform.org
	
For more documentation and latest updated news, please visit our websites:
	www.exoplatform.com
	www.blog.exoplatform.org
	
If you have questions, please send a mail to the list exoplatform@objectweb.org.

Thank your for using eXo Content product!
The eXo Content team.
