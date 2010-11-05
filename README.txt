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
5. Running
6. Release notes


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
	* EAR compliance with EPP5.1-ER3
	* CKEditor3 compatibility, Simple Json based UITable for dialogs/views
	* Rest service for cleanName in nodes
	* Authoring Plugin
	* L11n (as Tech Preview for RedHat)
	* PDF View for almost document: MS Document, Openfice Document
	* InContext Editing
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
		This will explain you how to build a package of WCM 2.1.1 with Tomcat.
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
		* JBoss package:
		** mvn clean install in /delivery/wcm
		** Open /delivery/wcm/jboss-package/target/ then you will see a zip file named eXoContent-2.1.1-jboss-package-jboss.zip
		
		* Workflow ear(optional):
		** mvn clean install in /packaging/workflow/ear
        
		*****************
		* STARTING:
		*****************		
		* On Tomcat: go to the tomcat directory (or unzip the archive in your favorite location) and execute 'bin/gatein.sh start' ('bin/gatein.bat start' on Windows)
		* On EPP5: unzip eXoContent-2.1.1-jboss-package-jboss.zip and copy everything to folder /jboss-epp-5.0/jboss-as/server/default/deploy (you can copy the workflow ear into this folder if needed)
			Go to /jboss-epp-5.0/jboss-as/bin and execute './run.sh' ('/run.bat start' on Windows)
	
5. RUNNING
---------------------------------------------------
5.1 Open your web browsers, now eXo Content can run on FireFox 2 or newer, Internet Explorer 7 or newer (we recommend using FireFox 3+ or Internet Explorer 7+ for the best result)
		Navigate to URL: http://localhost:8080/ecmdemo/public/classic

5.2 When the page has been loaded, click "Login" on the top right corner. Specify the username "john" and the password "gtn".
    Or you can simply by click into the text "Login as John"

6. RELEASE NOTES 
---------------------------------------------------

Release Notes - exo-ecms - Version wcm-2.1.1

** Bug
    * [ECMS-1000] - [DMS] Show exception in cygwin when click remove item icon when don't select categories you want to add for document
    * [ECMS-1084] - Can not select no more content for an SCV after selecting a content once
    * [ECMS-1147] - UI error when open Advance in Preferences setting form
    * [ECMS-1164] - Drivers Pane is not shown and then Site Explore is broken after change permission of a driver with any permission
    * [ECMS-1167] - Can't login in special case
    * [ECMS-1183] - Unknown error when edit File plan second time
    * [ECMS-1246] - Error UI when print a document in special case
    * [ECMS-1268] - Search: search query does not appear in simple search box in WCM
    * [ECMS-1302] - CLV : Wrong url generated jcr:frozenNode
    * [ECMS-1316] - Edit Locked Node from front shouldn't be possible
    * [ECMS-1378] - When a link is generate, selected text is replace
    * [ECMS-1403] - Multiselector fields reset on selction of categories
    * [ECMS-1417] - WEBDAV URL doesn't work in PCV after publication
    * [ECMS-1422] - Node is locked error on saving content
    * [ECMS-1444] - Can't view document when this document was inserted into other document by  FCK Content Selector
    * [ECMS-1450] - Incorrect display of names when name is too long when uploading a file
    * [ECMS-1456] - Bad/missing translation in popup help message
    * [ECMS-1471] - Don't show the preference and add content icon when the CLV enable to show infor bar
    * [ECMS-1479] - Can't Edit document in special case
    * [ECMS-1489] -  Still keep create document form when change drive
    * [ECMS-1491] - Comment of Article document is not shown in CLV
    * [ECMS-1493] -  Exception when add properties to search node in SCV
    * [ECMS-1495] - Cannot view an article document on SCV which has attachment
    * [ECMS-1496] - Show target blockid when setting for new driver
    * [ECMS-1511] - Duplicate SE templates if edit and save from Site administrator
    * [ECMS-1518] - Split friendly conf and content conf in ecmdemo
    * [ECMS-1519] - Cannot replace a binary from within the dialog
    * [ECMS-1520] - DocumentsTemplate doesn't work as before
    * [ECMS-1527] - Taxonomy/category field not populating on Edit
    * [ECMS-1528] - Name of content selector is not correct 
    * [ECMS-1529] - Upload file icon is not show at Content selector form
    * [ECMS-1532] - Remove pull-parser-2.jar from wcm's ears to fix problem with wrong parser of gadget
    * [ECMS-1539] - Date Format error when create new document (with French Language selected)
    * [ECMS-1541] - Test failure on wcm 2.1.x branches
    * [ECMS-1542] - Browser does not go back to right page after editing content of SCV, managing contents of CLV
    * [ECMS-1544] - Remove upload fields cannot work properly  with nt:file child nodes 
    * [ECMS-1568] - Cluster configuration is wrong for some of WCM workspaces
    * [ECMS-1587] - Error message on edit File Plan
    * [ECMS-1591] - Invalid Wysiwyg toolbar in FreeWebLayout template
    * [ECMS-1594] - Add selectDestPath with category selector  launch an exception
    * [ECMS-1613] - CLONE -Backport to REL- Node is locked error on saving content
    * [ECMS-1625] - Exception when create new node
    * [ECMS-1628] - Some bugs in the new classic home page
    * [ECMS-1630] - Update main pom in branches to upgrade version of shindig
    * [ECMS-1644] - Edit ECM Templates : a new template is created... 

** Documentation
    * [ECMS-1297] - Inside WCM Explorer
    * [ECMS-1298] - Extensions
    * [ECMS-1335] - Getting started with eXo WCM

** Improvement
    * [ECMS-345] - Improve Upload File function in the Content Selector form
    * [ECMS-1410] - Configuration of eXo JCR improvements

** Task
    * [ECMS-374] - Review all resource bundle files
    * [ECMS-983] - Review and correct the messages in resource bundle files
    * [ECMS-1515] - Test WCM EARs on jboss epp 5.1.0-ER1
    * [ECMS-1517] - update README.txt
    * [ECMS-1553] - WCM 2.1.1 TC
    * [ECMS-1593] - JBoss EAR packaging



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
