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

You may find it helpful to see the details at wiki place of ECM
http://wiki.exoplatform.org/xwiki/bin/view/ECM/


TABLE OF CONTENTS
---------------------------------------------------
1. What is eXo ECM
2. What's new?
3. How to set up eXo ECM
4. Running
5. Release notes
6. Migration guide


1. WHAT IS EXO ECM
---------------------------------------------------
	Enterprise Content Management is the strategies, methods and tools used to capture, manage, store, preserve and deliver 
contents and documents related to organizational processes with the purpose of improving operational productivity and efficiency. 
ECM tools and strategies allow the management of an organization's unstructured information, whether that information exists. 
The eXo Platform ECM portlet provides you with a portal solution that can help you achieve these processes easily and it is carefully 
designed so that you can leverage your business content across all formats for competitive gain. An environment for employees is also 
provided to share and collaborate digital contents as well as delivering a comprehensive unified solution with rich functionalities.
ECM consists of three parts: 
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
	*  PDF View for almost document: MS Document, Openfice Document
	Find more details at the RELEASE NOTEs part

3. HOW TO SET UP EXO ECM
---------------------------------------------------
eXo Enterprise Content Management requires the Java 2 Standard Edition Runtime
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

4. RUNNING
---------------------------------------------------
4.1 Open your web browsers, now eXo WCM can run on FireFox 2 or newer, Internet Explorer 7 or newer (we recommend using FireFox 3+ or Internet Explorer 7+ for the best result)
		Navigate to URL: http://localhost:8080/ecmdemo

4.2 When the page has been loaded, click "Login" on the top right corner. Specify the username "root" and the password "gtn".

5. RELEASE NOTES 
---------------------------------------------------

Release Notes - exo-ecms - Version wcm-2.0.0-CR01

** Bug
    * [ECMS-18] - other workspace then collaboration doesn't work in CLV
    * [ECMS-37] - can't view symlink by webdav if this symlink is related to a folder which have 2 child nodes
    * [ECMS-69] - Cannot create more than one Editor field in FormGenerator
    * [ECMS-224] - Error occurs when trying to search a web content
    * [ECMS-229] - Map network drive and WEBDAV do not work
    * [ECMS-233] - Don't use userId as a parameter in the request URL of Content Selector
    * [ECMS-239] - Impossible to change a Web content illustration
    * [ECMS-240] - IE7: Lost some characters at ACME site
    * [ECMS-241] - Title of Site Explorer is changed when perform to open a node
    * [ECMS-242] - Unknown error when choose existing content for page in special case
    * [ECMS-245] - Edit form of Content list viewer portlet is not disappeared when click Cancel button
    * [ECMS-246] - error page is shown when go to manage site page if standing at new site
    * [ECMS-278] - Can't create document in category folder
    * [ECMS-279] - CLV portlet is broken
    * [ECMS-280] - Unknown error if send letter without internet connection
    * [ECMS-283] - Can't add symlnk for article
    * [ECMS-284] - link of document on folder which was add exo:taxonomy action is still displayed content when this document was deleted 
    * [ECMS-285] - Search for nodes in the Advance search of DMSAdministration Driver
    * [ECMS-289] - Content Explorer does not work
    * [ECMS-292] - Change CLV default target page
    * [ECMS-297] - Error UI in Single content viewer form. see file attach
    * [ECMS-298] - unknown error and throw exception when click on "Abort" button in Single content viewer form
    * [ECMS-299] - WebDAV doesn't work
    * [ECMS-305] - Some content selector's bugs
    * [ECMS-311] - IE7: Login function is invalid
    * [ECMS-328] - Error when add new property for node with type is Binary
    * [ECMS-339] - Any shouldn't see Document folder in groups contents
    * [ECMS-342] - Problems with Sorting Mechanism
    * [ECMS-344] - In Content Selector form, Uploaded file is not saved 
    * [ECMS-346] - can't configure the content creator
    * [ECMS-349] - View Image in the other language is not displayed
    * [ECMS-350] - can not delete portal classic when create new extension
    * [ECMS-355] - Can not view media file
    * [ECMS-356] - Can not play  file with media format
    * [ECMS-365] - Bug when add publishing process for document
    * [ECMS-367] - can't see the title when editing it in the acme website.
    * [ECMS-368] - when editing the configuration of an article, after clicking on cancel, the article disapear
    * [ECMS-369] - Incorrect Window Title 
    * [ECMS-372] - Content of article disappear after switch to edit mode
    * [ECMS-373] - Label of ndoetype name doesn't display on nodetype selector component
    * [ECMS-377] - Cannot upload OR create a document with child nodes inside a taxonomy node
    * [ECMS-378] - Show content is empty  when print a web content
    * [ECMS-379] - Selected contents list is not shown in CLV config
    * [ECMS-380] - QuickEdit.js is loaded to many times... 
    * [ECMS-381] - Manage Categories doesn't work without System tree
    * [ECMS-383] - PCV does not support Edit/Live mode switch (blank page when switching)
    * [ECMS-384] - Improve presentation of the banner to avoid 2 lines printing when we have long description
    * [ECMS-385] - Site Explorer should show the "Document Viewer" by default and not the "File View" on Webcontent
    * [ECMS-386] - French Translation: in the CLV/SCV edit preference we should use the word 'Modèle' instead of 'template'
    * [ECMS-389] - Bad French Translation for "register" should be "S'inscrire" or "Inscription" instead of "Enregistrer" 
    * [ECMS-390] - Bad French Translation for "register" should be "S'inscrire" or "Inscription" instead of "Enregistrer" 
    * [ECMS-395] - Cannot print a document
    * [ECMS-397] - Change search result template to avoid showint ugly URLs
    * [ECMS-401] - Change languages is invalid
    * [ECMS-405] - No validate argument for calendar fields
    * [ECMS-422] - broken UI when compare Version
    * [ECMS-424] - Add External Matadata form is been empty when add matadata for node  in special case
    * [ECMS-427] - JobSchedulerService doesn't work
    * [ECMS-428] - Authoring plugin cannot run
    * [ECMS-433] - Lost  Edit Comment icon
    * [ECMS-436] - New View is lost name when is displayed at add driver form
    * [ECMS-437] - Still show title, name, illustration ... of webcontent/document(in draft status) when view in Live mode in News page
    * [ECMS-455] - UITable js object doesn't allow return carret
    * [ECMS-457] - Authoring CronJob exception when starting tomcat

** Improvement
    * [ECMS-30] - FE : Visible nodetypes on the left panel
    * [ECMS-253] - FormGenerator : Add checkbox and radio fields
    * [ECMS-290] - In CLV configuration, when user select a  content in search results, inform him that his content is correctly selected
    * [ECMS-293] - L11n : MultiLanguage Service could be Symlink aware
    * [ECMS-294] - WCMComposer : new filters
    * [ECMS-348] - Some languages labels are display too short to understand
    * [ECMS-351] - Being viewed language title should be bold rather than the default one
    * [ECMS-408] - Refactor Content list viewer
    * [ECMS-426] - UIAction : rename popup
    * [ECMS-447] - Acme webcontent should be in web contents folder
    * [ECMS-448] - SE : Support for i18n and references in Relation panel
    * [ECMS-451] - Add Event content with map

** New Feature
    * [ECMS-314] - CKeditor: Add new plugin syntaxhighlighter
    * [ECMS-359] - Test and Package Authoring Plugin
    * [ECMS-430] - ext : Staging cronjob on Authoring extension
    * [ECMS-450] - Add support for H264 videos using HTML5 video tag


** Task
    * [ECMS-215] - Review modules not in the reactor
    * [ECMS-226] - Upgrade to jBPM 3.2.x
    * [ECMS-438] - Release WCM 2.0.0-CR01
    * [ECMS-439] - EAR : add "static" project
    * [ECMS-440] - EAR : check WCM+WKF
    * [ECMS-442] - Make sure WCM 2.0 works on top of EPP5
    * [ECMS-445] - Backport Dialog template for nt:file from WCM 1.2
    * [ECMS-446] - Make System the default export/import format instead of Document
    * [ECMS-453] - Update Release notes files


** Sub-task
    * [ECMS-94] - Create new hierarchy
    * [ECMS-95] - Merge/Reorganize modules content
    * [ECMS-118] - Rename artifactIDs
    * [ECMS-157] - Adding an exo:createRSSFeedAction is not working
    * [ECMS-295] - Query filters and impl
    * [ECMS-360] - Add support for editing without popup
    * [ECMS-366] - Language filters and impl
    * [ECMS-443] - apply to WCM ear

6. MIGRATION GUIDE
---------------------------------------------------

** Other resources and links
	Company site        http://www.exoplatform.com
	Community JIRA      http://jira.exoplatform.org
	Comminity site      http://www.exoplatform.org
	Developers wiki     http://wiki.exoplatform.org
	Documentation       http://docs.exoplatform.org 
	
For more documentation and latest updated news, please visit our websites:
	www.exoplatform.com
	www.blog.exoplatform.org
	
If you have questions, please send a mail to the list exoplatform@objectweb.org.

Thank your for using eXo WCM product!
The eXo WCM team.
