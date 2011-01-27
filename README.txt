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
	* EAR compliance with EPP5
	*	CKEditor3 compatibility, Simple Json based UITable for dialogs/views
	*	Rest service for cleanName in nodes
	*	Authoring Plugin (as Tech Preview for RedHat)
	*	L11n (as Tech Preview for RedHat)
	*  PDF View for almost document: MS Document, Openfice Document
	Find more details at the RELEASE NOTEs part

3. HOW TO SET UP EXO CONTENT
---------------------------------------------------
eXo Content requires the Java 2 Standard Edition Runtime
Environment (JRE) or Java Development Kit version 6.x

3.1 Install Java SE 1.6 (Java Development Kit)
Based on JavaEE, our WCM runs currently fine with version 1.6 so if you are using newer version, please download and install this version to make WCM works fine. We will support newer version of Java very soon.

3.2 Download the latest eXo Web Content Management version from: https://sourceforge.net/projects/exo-wcm/

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
		This will explain you how to build a package of WCM 2.1.2-CR01 with Tomcat.
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
		
		* ECMDEMO ear
		** mvn clean install in /packaging/ecmdemo/ear
		
		* Workflow ear(optional):
		** mvn clean install in /packaging/workflow/ear

		*****************
		* STARTING:
		*****************		
		* On Tomcat: go to the tomcat directory (or unzip the archive in your favorite location) and execute 'bin/gatein.sh start' ('bin/gatein.bat start' on Windows)
		* On EPP5: Copy WCM extension ear and ECMDEMO ear to folder /jboss-epp-5.0/jboss-as/server/default/deploy 
			Go to /jboss-epp-5.0/jboss-as/bin and execute './run.sh' ('/run.bat start' on Windows)
	
5. RUNNING
---------------------------------------------------
5.1 Open your web browsers, now eXo Content can run on FireFox 2 or newer, Internet Explorer 7 or newer (we recommend using FireFox 3+ or Internet Explorer 7+ for the best result)
		Navigate to URL: http://localhost:8080/ecmdemo

5.2 When the page has been loaded, click "Login" on the top right corner. Specify the username "root" and the password "gtn".

6. RELEASE NOTES 
---------------------------------------------------

Release Notes - exo-ecms - Version wcm-2.1.2-CR01

** Bug
    * ECMS-1794	NPE when rendering edit mode of SCV portlet
    * ECMS-1786	UI error when signin with "mary" user and go to site explorer
    * ECMS-1785	Missing div close tag when addQuickEditDiv in class UICLVPresentation.java
    * ECMS-1779	All templates content has been reset after restart server
    * ECMS-1778	Some tests are failing on the hudson
    * ECMS-1777	Error message "TypeError: eXo.webui.UIPopup is undefined" appear when uploading content from ContentSelector
    * ECMS-1774	Mutisecetor field rest on selection of category or repeating field
    * ECMS-1772	Some pdf's are not transferred
    * ECMS-1771	Multiselector field resets on selection of category or repeating field
    * ECMS-1770	ERROR message in the console when startup Workflow
    * ECMS-1769	error in addSelectBox function in UIDialogForm
    * ECMS-1767	'Some constraints violated' exception for templates having child node
    * ECMS-1766	CLV/PCV mixes frozen nodes and base version
    * ECMS-1762	Upload-fields-are-non-editable-in-workflow-if-preselected
    * ECMS-1761	Wrong code in ContentSelector.js
    * ECMS-1758	Do not set __system as last user when going from staged to published state
    * ECMS-1757	Multivalue field value is not filled after saving file.
    * ECMS-1753	Remove dependency of portal xml parser and add dependency for the one from WCM
    * ECMS-1751	Cannot start Workflow extension in EPP51
    * ECMS-1741	The order of services loading are wrong with Ubuntu
    * ECMS-1740	SE : Dialog consumes too much memory
    * ECMS-1708	Exception when modify content from classic site
    * ECMS-1694	Cannot add symlink in a workspace different of node's workspace
    * ECMS-1691	Umbrella for the side effects after the changing of JCR
    * ECMS-1684	translation node with "any" access permession are not visible to public user after restat the server
    * ECMS-1681	A link that reference documen in "ecms" is died
    * ECMS-1671	System files and directories must be hidden
    * ECMS-1667	Uploaded file's name is not displayed in Edit form
    * ECMS-1665	Wrong label in remove tag permission
    * ECMS-1664	Wrong Log class in SendMailScript.groovy
    * ECMS-1663	Missing label in Tag deletion js popup
    * ECMS-1662	nomal and hotest bad styles
    * ECMS-1659	[ECMS 2.1.2] Edit content in SCV porlet (Test_SNF_ECMS_04)
    * ECMS-1652	Rendering translations in PCV portlet
    * ECMS-1650	Do not create workflow tables in database when the "workflow" profile is not activated
    * ECMS-1648	Problem of multi-languages
    * ECMS-1644	Edit ECM Templates : a new template is created...
    * ECMS-1637	Setting for Category Navigation is not working
    * ECMS-1634	Textarea type is not saved
    * ECMS-1632	Category field not-populated when edited in workflow UI-task-form
    * ECMS-1630	Update main pom in branches to upgrade version of shindig
    * ECMS-1628	Some bugs in the new classic home page
    * ECMS-1627	The checkbox "is document template" is not checked anymore when create new template in ECMAdmin
    * ECMS-1626	CLV - Show in page: unable to select a page
    * ECMS-1625	Exception when create new node
    * ECMS-1624	WCM 2.1.1 doesn't start on EPP 5.0.1
    * ECMS-1621	Cannot create CLV template, or equivalent from the File Explorer
    * ECMS-1618	Unknown error when configure clv/scv when browsing directories with special characters (using IE7)
    * ECMS-1616	Right click does not work if a drive has not been selected
    * ECMS-1613	CLONE -Backport to REL- Node is locked error on saving content
    * ECMS-1611	Exception when edit document with multi-value reference field
    * ECMS-1594	Add selectDestPath with category selector launch an exception
    * ECMS-1591	Invalid Wysiwyg toolbar in FreeWebLayout template
    * ECMS-1588	Cannot used action for content-publishing process
    * ECMS-1587	Error message on edit File Plan
    * ECMS-1586	Can not view link on portlet
    * ECMS-1585	Do not display the upload field on EPP
    * ECMS-1584	Can not auditing for node
    * ECMS-1583	Cannot create a new Newsletter on EPP 5.1
    * ECMS-1582	Still show "null" in FCKEditor when user add language for documents
    * ECMS-1568	Cluster configuration is wrong for some of WCM workspaces
    * ECMS-1561	EPP5.1: Can not export node
    * ECMS-1552	Picture is still shown at Published mode while this picture is draft
    * ECMS-1551	Category Fields not Editable in Edit Mode, while Editable is true
    * ECMS-1547	Right click in Site Explorer is not working
    * ECMS-1544	Remove upload fields cannot work properly with nt:file child nodes
    * ECMS-1543	Error "Item already exists" when trying to inline edit a content.
    * ECMS-1542	Browser does not go back to right page after editing content of SCV, managing contents of CLV
    * ECMS-1541	Test failure on wcm 2.1.x branches
    * ECMS-1539	Date Format error when create new document (with French Language selected)
    * ECMS-1532	Remove pull-parser-2.jar from wcm's ears to fix problem with wrong parser of gadget
    * ECMS-1529	Upload file icon is not show at Content selector form
    * ECMS-1528	Name of content selector is not correct
    * ECMS-1527	Taxonomy/category field not populating on Edit
    * ECMS-1525	Access Denied in jcr:content with anonim__ permission on published content
    * ECMS-1521	Multi selected exception when we add to dc:subject property
    * ECMS-1520	DocumentsTemplate doesn't work as before
    * ECMS-1519	Cannot replace a binary from within the dialog
    * ECMS-1518	Split friendly conf and content conf in ecmdemo
    * ECMS-1511	Duplicate SE templates if edit and save from Site administrator
    * ECMS-1497	Click to "BackToFront" icon, the node has not been released lock.
    * ECMS-1496	Show target blockid when setting for new driver
    * ECMS-1495	Cannot view an article document on SCV which has attachment
    * ECMS-1494	Exception after rename File document while it is being locked
    * ECMS-1493	Exception when add properties to search node in SCV
    * ECMS-1492	Exception when view content of File document in search result of SCV/CLV
    * ECMS-1489	Still keep create document form when change drive
    * ECMS-1485	Site Explorer should not show drives when option "jailed" is chosen
    * ECMS-1480	Still show content of document althought it was deleted
    * ECMS-1479	Can't Edit document in special case
    * ECMS-1471	Don't show the preference and add content icon when the CLV enable to show infor bar
    * ECMS-1469	Media file can be attached in WYSIWYG irrespective of permissions
    * ECMS-1458	can't download file .pdf
    * ECMS-1457	Broken URL on Webdav view button for js or css files
    * ECMS-1456	Bad/missing translation in popup help message
    * ECMS-1450	Incorrect display of names when name is too long when uploading a file
    * ECMS-1447	Don't show Search icon in Mange Node type (IE7 & IE6)
    * ECMS-1444	Can't view document when this document was inserted into other document by FCK Content Selector
    * ECMS-1443	Remove category doesn't work in edit workflow tab
    * ECMS-1438	IE7: Error displaying in Add tab form when add new view
    * ECMS-1437	IE7: Error displaying button in Metadata form when edit a upload file
    * ECMS-1432	IE7: Error with Newsletter entry form
    * ECMS-1424	Unknown error on clearing publication date of new version
    * ECMS-1422	Node is locked error on saving content
    * ECMS-1417	WEBDAV URL doesn't work in PCV after publication
    * ECMS-1403	Multiselector fields reset on selction of categories
    * ECMS-1394	Missing resource for JCRExplorerPortlet
    * ECMS-1392	Can't search with second keyword
    * ECMS-1378	When a link is generate, selected text is replace
    * ECMS-1322	WCM extension populates sample users
    * ECMS-1316	Edit Locked Node from front shouldn't be possible
    * ECMS-1302	CLV : Wrong url generated jcr:frozenNode
    * ECMS-1277	CLV: need to switch from "edit" to "view" mode after saving configuration of CLV
    * ECMS-1274	WYSIWYG not shown completely When content is edited from workflow task
    * ECMS-1268	Search: search query does not appear in simple search box in WCM
    * ECMS-1261	Publication state not correctly displayed
    * ECMS-1246	Error UI when print a document in special case
    * ECMS-1183	Unknown error when edit File plan second time
    * ECMS-1167	Can't login in special case
    * ECMS-1164	Drivers Pane is not shown and then Site Explore is broken after change permission of a driver with any permission
    * ECMS-1156	No vertical scroll bar for webContent documents
    * ECMS-1147	UI error when open Advance in Preferences setting form
    * ECMS-1094	Can't add root category for node while upload file
    * ECMS-1088	Cannot create new document in a folder which have SendMailActionScript
    * ECMS-1084	Can not select no more content for an SCV after selecting a content once
    * ECMS-1039	Correct the message when a content list is empty
    * ECMS-1023	DMS: Can not cut/paste 1 locked node but can drag & drop
    * ECMS-1003	Remove group and sites tags from the Tag extension
    * ECMS-1000	[DMS] Show exception in cygwin when click remove item icon when don't select categories you want to add for document
    * ECMS-979	WCM- Delete uploaded files works invalidly
    * ECMS-966	Error when create category after deleting group/user
    * ECMS-965	Document folder and Application data folder are not shown when choose target node at driver : /platform/user
    * ECMS-964	IE7 [DMS] can't create Podcast document
    * ECMS-808	WCM: Multi-Content selector form is reloaded
    * ECMS-309	WCM Navigation Portlet does not render sub menus

** Improvement
    * ECMS-1715	Fix EPPSP packaging
    * ECMS-1714	Add public portlet markup cache layer
    * ECMS-1705	out of memory errors When uploading content with large size
    * ECMS-1692	Shouldn't show alert message when choose content to add in the list from ContentSelector
    * ECMS-1683	remove some unncessary jars
    * ECMS-1607	Update the default application in Content application category
    * ECMS-1523	Provide visual feedback in content selector while loading list of drives
    * ECMS-1410	Configuration of eXo JCR improvements
    * ECMS-345	Improve Upload File function in the Content Selector form

** Delivery Item
    * ECMS-1783	Remove ear-plf from SVN

** Feedback
    * ECMS-1555	UIPageSelector Component behavior

** Documentation
    * ECMS-1608	Add cleanupPublication property explanation in the refguide
    * ECMS-1604	Refguide : explain the Authoring plugin
    * ECMS-1482	Java Services
    * ECMS-1335	Getting started with eXo WCM
    * ECMS-1301	FAQ
    * ECMS-1298	Extensions
    * ECMS-1297	Inside WCM Explorer

** Task
    * ECMS-1791	Check test failing on Hudson
    * ECMS-1789	Review JBEPP issues fixed by WCM 2.1.2-CR01 release
    * ECMS-1742	Test WCM branches 2.1.2-SNAPSHOt on top of EPP 5.1-ER4
    * ECMS-1737	Review and apply the patches which proposed last sprint
    * ECMS-1735	Add sources packaging distribution
    * ECMS-1724	Add dependency for icu4j in core/connector and remove the build for core/ext
    * ECMS-1696	Replace /command by /upload /download
    * ECMS-1682	Remove wcm-config from wcm-extension EAR
    * ECMS-1680	PERF : Drive selection in content selector dialog is too slow
    * ECMS-1679	Add the missing dependency in wcm 2.1.x
    * ECMS-1678	Upgrade version of commons from 1.0.1-SNAPSHOT to 1.0.2-SNAPSHOT
    * ECMS-1654	Remove bin folder in workflow bp-bonita-holiday
    * ECMS-1635	upgrade chromatic version from 1.0.3 to 1.0.4 in main pom.xml file of WCM 2.1.x
    * ECMS-1615	Get Email settings from configuration.properties
    * ECMS-1595	Update home page
    * ECMS-1593	JBoss EAR packaging
    * ECMS-1558	default portal removal
    * ECMS-1553	WCM 2.1.1 TC
    * ECMS-1540	List all scheduler jobs of WCM project
    * ECMS-1517	update README.txt
    * ECMS-1515	Test WCM EARs on jboss epp 5.1.0-ER1
    * ECMS-1350	Migration of a customer project (WCM 2.0.0 to WCM 2.1.0)
    * ECMS-1287	Write a new Developer Reference Guide
    * ECMS-983	Review and correct the messages in resource bundle files
    * ECMS-374	Review all resource bundle files

** Sub-task
    * ECMS-1748	"ECMS-1742 Register ""*"" as a membership type."
    * ECMS-1729	"ECMS-1715 Test on EPP 5.1"
    * ECMS-1722	"ECMS-1666 Changes in permissions are not taken in consideration by SymLinks"
    * ECMS-1706	"ECMS-1666 CKEditor probloem with table setting"
    * ECMS-1703	"ECMS-1666 End of Tag not exist (W3C)"
    * ECMS-1702	"ECMS-1666 Exception with Content List Viewer when select the folder that has name contains special character"
    * ECMS-1676	"ECMS-1673 Update README and Release notes"
    * ECMS-1675	"ECMS-1673 Fix the issue related to new homepage"
    * ECMS-1674	"ECMS-1673 Upgrade version of WCM 2.1.1_REL (from 2.1.2-SNAPSHOT to 2.1.1.1-SNAPSHOT)"
    * ECMS-1670	"ECMS-1633 Write specification"
    * ECMS-1669	"ECMS-1666 Analyze the workload this issue"
    * ECMS-1609	"ECMS-1107 Test the described scenario (using external jar)"
    * ECMS-1460	"ECMS-1298 REST Services"
    * ECMS-529	"ECMS-374  Translation - English"
    * ECMS-528	"ECMS-374  Translation - Arabic"
    * ECMS-527	"ECMS-374  Translation - French"

Release Notes - exo-ecms - Version wcm-2.1.0-GA

** Bug
    * ECMS-1481	Can't choose folder path for CLV or SCV at new page
    * ECMS-1476	Exception when choose folder path of SCV & CLV
    * ECMS-1467	Exception with modify CLV preference to use TagClould.gtmpl
    * ECMS-1453	FUNCTIONAL REGRESSION: Cannot edit CLV template from Site Explorer
    * ECMS-1428	Cannot print document
    * ECMS-1427	CLONE -Do not show the draft icon
    * ECMS-1426	Test failure in org.exoplatform.services.ecm.dms.template.TestTemplateService
    * ECMS-1421	Don't show the quick edit of a SCV in edit mode when this portlet was configured to show infor bar
    * ECMS-1416	in SCV, the content's title doesn't updated when select a found document.
    * ECMS-1414	Browser does not go back to right page after editing content in PCV mode of SCV
    * ECMS-1411	Do not release JBoss packages
    * ECMS-1408	Cannot publish a content twice
    * ECMS-1406	When changing the portlet preferences CLV the portlet (or page) should be refreshed to reflec the change
    * ECMS-1402	Cannot edit a content correctly
    * ECMS-1401	Still showing deleted documents and files
    * ECMS-1397	Some items are not translated in CLV preferences.
    * ECMS-1389	html to QuickEdit should be hidden in CLV Template
    * ECMS-1388	Exception when adding CLV
    * ECMS-1387	Automatic title detection doesn't work in CLV
    * ECMS-1385	Edit mode doesn't work on Detail page
    * ECMS-1384	thumbnail origin service doesn't work in some contexts
    * ECMS-1383	Tags don't work in CLV
    * ECMS-1379	Link generation not ok in content edition
    * ECMS-1376	FUNC REGRESSION : Edit mode: we should indicate the status of te document (draft, staging)...
    * ECMS-1373	Symlink of some document types is not added when add document in acme drive
    * ECMS-1372	Some document type can not be searched in CLV
    * ECMS-1367	Can not show image when view in CLV
    * ECMS-1363	Content of document is not shown in new portlet of News page
    * ECMS-1319	The lock has not been released after sign out
    * ECMS-1315	CLONE -Sites Explorer can't displayed when view a new template
    * ECMS-1310	The HTTP header ifModifiedSince needs to be given to the jcr version of the WebDavServiceImpl to enable the cache-control
    * ECMS-1309	Resources served by the JCR, aren't cached by the browser
    * ECMS-1308	"Edit Document" doesn't work
    * ECMS-1307	SCV,CLV : go in the right place when selecting a node
    * ECMS-1304	Cannot save a web content
    * ECMS-1291	Can't go back to father node when using InContext editing
    * ECMS-1290	DocumentView is not always the default view
    * ECMS-1286	CLONE -Remove Data in dialog template
    * ECMS-1265	CLONE -[WCM] Content of CLV is not changed after editing path
    * ECMS-1262	Cannot set Folder path in Content List portlet properties
    * ECMS-1260	Problem when displaying drives
    * ECMS-1259	[WCM]Message is not shown to alert configure successfully
    * ECMS-1258	CLONE -Skin error exception on starting server
    * ECMS-1257	[WCM] Can not select a new document when create [SCV] page
    * ECMS-1256	CLONE -Cannot modify the metadata
    * ECMS-1255	CLONE -Cannot display a published file
    * ECMS-1253	Added taxonomy is not displayed in Referenced categories tab in Add category form
    * ECMS-1251	Content List viewer isn't updated after edit
    * ECMS-1250	Edit uploaded file isn't updated
    * ECMS-1249	Show message error when quick edit a content in CLV
    * ECMS-1248	Error UI when add Newsletter. See file at
    * ECMS-1247	Can not see node that links to no page when insert link to a site page into a web content
    * ECMS-1245	[IE7] Edit Mode does not work
    * ECMS-1244	[IE7] The FCK Editor is not displayed after saving an article document
    * ECMS-1243	Error UI when file name is long. see file attach
    * ECMS-1242	[CLV] The Content List viewer portlet preferences panel is not fully internationalized.
    * ECMS-1237	Css problem in file explorer with french language
    * ECMS-1235	CLONE -IE7: UI error when edit Acticle
    * ECMS-1234	Authoring dashboard is not deployed
    * ECMS-1231	CLONE -Actions for SCV and CLV do not display in IE7
    * ECMS-1228	Cannot edit in context twice
    * ECMS-1223	CLONE -Simple search in "File explorer" portlet
    * ECMS-1222	IE: Content of Free web layout content and Picture on head layout web content are not changed after Editing
    * ECMS-1219	CLONE -Content of searched doucment is not shown in public mode
    * ECMS-1218	CLONE -Harmonize Preferences panels (SCV, CLV, Explorer)
    * ECMS-1212	Some strings in CLV edit mode are not well defined
    * ECMS-1211	NPE on CLV By Contents
    * ECMS-1210	Empty Date fields set with current date after edit
    * ECMS-1209	error message is shown when delete comment of Free web content layout
    * ECMS-1208	[FormGenerator] The Reset functionnality in FCKEditor does not work
    * ECMS-1203	[WCM] UI Error in upload file
    * ECMS-1202	CLONE -Taxonomy/category field not populating on Edit
    * ECMS-1201	ContentList portlet doesn't reflect actual state of groovy template
    * ECMS-1193	IE7 [DMS] UI error at Add View form
    * ECMS-1187	Only view folder on WebDAV by creating network place
    * ECMS-1186	CLONE -ChangeStateCronJobImpl logs too much
    * ECMS-1185	Can't add language for Article
    * ECMS-1159	Unknown error when create 1 template by Form Generator
    * ECMS-1158	FolderData.getPath() returns object's path without root folder part. Investigate how to this affects to WCM compartibility.
    * ECMS-1155	Gadget cannot display well on documents when expried
    * ECMS-1151	IE7: UI error at Authoring Dashboard
    * ECMS-1150	NPE when choose a wrong template for CLV
    * ECMS-1149	Display error on Site Explorer
    * ECMS-1148	Can't right click on folder at defautl driver
    * ECMS-1142	rightclick does not work if we don't show side bar
    * ECMS-1140	Exception when remove the "/" character at the SE Address bar and hit Enter
    * ECMS-1139	Editor with Top Bar
    * ECMS-1138	Explorer without Filter Bar
    * ECMS-1137	Views for contents are broken
    * ECMS-1136	Edit dialog from UI is not updated
    * ECMS-1135	Has some wrong label
    * ECMS-1134	My Draft content doesn't update after a document in Draft is removed.
    * ECMS-1133	Left sidebar jump out of the panel
    * ECMS-1132	First time info message hides the tree
    * ECMS-1131	Show Javascript message when changin the type of document
    * ECMS-1130	Left Panel is disappeared when clicking on an item in My Draft Content
    * ECMS-1128	Richtext component : views are not applied
    * ECMS-1127	Tree explorer does not change after selecting collaboration drive
    * ECMS-1126	Can't save view/dialogs from UI
    * ECMS-1125	Select document type drop down box doesn't appear when add new document
    * ECMS-1124	Event should use Richtext webui component
    * ECMS-1123	Explorer : default view is popup
    * ECMS-1122	InContext editing : 1px border to remove in published mode
    * ECMS-1121	Preference form has been broken when click to advance button
    * ECMS-1119	CurrentPortalContainer from CronJob doesn't work in AuthoringPlugin
    * ECMS-1118	Explorer : Data in CKEditor instances are not saved
    * ECMS-1117	WCMComposer returns twice the same contents
    * ECMS-1116	ui + javacript problems
    * ECMS-1105	IE7: UI error at Preferences setting form, see attached filed
    * ECMS-1104	Error when creating new content from Front side
    * ECMS-1103	Error when select mutil or one content
    * ECMS-1102	error message is shown when click at attachment of sample node
    * ECMS-1101	Default content of File is "null" and content of File is not shown
    * ECMS-1087	Review "Ownedw by me" logic in SE
    * ECMS-1078	Cannot import and display webcontents by config if they have publication state data
    * ECMS-1074	Sites Explorer refresh issue when create a new site
    * ECMS-1071	cannot see all the edit dialog in portlet edit window
    * ECMS-1056	WebDAV access does not work on some platforms
    * ECMS-1052	Provide translations for version actions
    * ECMS-1046	Cannot upload files when forced to use categories
    * ECMS-1036	[DMS] Show exception when click Add Sym link on Ubutu
    * ECMS-1033	[DMS] Can not open node properties form on Ubuntu
    * ECMS-1027	Show relations fails when viewing a referenced node
    * ECMS-1020	[DMS] Can not upload file into site exploror when run on Ubuntu
    * ECMS-1018	[DMS] Can not create some documents
    * ECMS-1013	Wrong code in UISelectPathPanel
    * ECMS-1007	First time you edit a existing web content warning popup is display
    * ECMS-1003	Remove group and sites tags from the Tag extension
    * ECMS-1002	Calendar appearing even on non-focus
    * ECMS-1001	Horizontal scroll bar and/or paginator not appearing in file explorer
    * ECMS-986	SE : onmouse hover doesn't work well in left bar
    * ECMS-959	Ignore accented characters in search
    * ECMS-944	Metadata : date field is reset after clicking on "+" to add a new value in another field
    * ECMS-930	A date field is shown instead of text arer field when property is multiple
    * ECMS-929	z issue in the "Event" dialog while selecting a date
    * ECMS-928	Front Editing should not allow all the user to "Publish" (Fast Publish) the content
    * ECMS-927	z-index issue when dragging a window over the admin toolbar
    * ECMS-922	Tree issue when adding a link to a document in FCKEditor in File Explorer
    * ECMS-921	Category Contents empty after selecting a content
    * ECMS-920	Content Detail edit in back office
    * ECMS-916	Category field for selection does not show up in editnode tab (content-publishing process).
    * ECMS-915	Form Generator : input label option required > unknow error
    * ECMS-914	Form Generator : text field instead of radio buttons or checkboxes
    * ECMS-912	Update XSD reference
    * ECMS-909	Hardcoded and wrong rest name in ContentView.gtmpl
    * ECMS-905	Test that new items can be managed by DAV
    * ECMS-898	javascript not loaded
    * ECMS-888	Perf issue on trunk
    * ECMS-887	WCM WebDAV service replaces file and content nodetype
    * ECMS-875	Can not switch tabs in Multi Languages form
    * ECMS-869	Property not found exception when calling obsolete state (before it reaches to published state) in Authoring publication
    * ECMS-859	Exception when select category by single content selector
    * ECMS-857	SCV and CLV do not work after creating a new site
    * ECMS-856	Missing method delete in WebDavService (core-services)
    * ECMS-838	Don't show image when search by Document
    * ECMS-835	Portlet POM Name should be different for exo-ecms-apps-portlet-administration and exo-ecms-apps-portlet-explorer
    * ECMS-822	error message is show when Click on Relation/Clipboard/Tag Cloud/Saved Searches icons on side bar
    * ECMS-820	Error in displaying 'Picture on head layout webcontent' in FCC portlet
    * ECMS-817	create a page, add a Form : Content Creator, the page will be blank in public mode
    * ECMS-816	name of taxonomy Tab is wrong when Add category to search
    * ECMS-815	Can't add category for document if User has not permission on a taxonomy tree
    * ECMS-814	Exception when select category of Classic/Events in Category portlet
    * ECMS-812	Content Publishing Process : ClassCastException
    * ECMS-811	build WCM2 on jboss
    * ECMS-807	Cannot display FCKEditor in web content form
    * ECMS-806	Have exception when add new letter
    * ECMS-804	Update of a new document (file) does not change the publication status
    * ECMS-802	Add resource bundle for IUNodeTypeSelector
    * ECMS-793	Show 'Page not found' when create 1 SCV page using exising content
    * ECMS-792	AuthoringPlugin : Use of ContainerName instead of CurrentContainer
    * ECMS-787	Category's title is not change
    * ECMS-784	Exception when open form to manage user in Newsletter Manage
    * ECMS-781	Resource bundle cannot load in publication plugins and authoring plugin
    * ECMS-779	Content explorer portlet breaks under "portal" container
    * ECMS-778	Error when viewing revision of 1 document has comment
    * ECMS-776	Only display the first uploaded file in PCLV portlet
    * ECMS-775	Cannot change permission of a content to set "read only permission
    * ECMS-774	Can not set an empty Date field after editing it once
    * ECMS-773	After change state, the author always is _system. Should be current user
    * ECMS-772	The category path is wrong when add a category to a document from "Manage categories" action
    * ECMS-771	Cannot access to Manage users from Newsletter Manager
    * ECMS-761	can not add a node into root category
    * ECMS-760	Change message when user edit content of page without 'Edit' permission
    * ECMS-759	Some problems with searching
    * ECMS-758	Add change state for OBSOLETE state in AuthoringPublicationPlugin
    * ECMS-757	Can't add document/upload file into category at driver DMS Administration
    * ECMS-748	WCM: Error when user edit document in private drive of another user
    * ECMS-733	[WCM] - Can not edit content detail in public mode after closing edit content form
    * ECMS-696	[DMS-improve] should allow shown sub-node of File plan & Kofax document when [enable JCR like structure]
    * ECMS-695	[WF] Content of document is not shown when edit content
    * ECMS-688	Cannot delete group
    * ECMS-676	NPE when NewsLetterEmailJob execute
    * ECMS-674	exo:processRecord didn't added by default when create file plan
    * ECMS-653	Left menu in Site explorer displays wrongly name of uploaded file if the name contains special character like '
    * ECMS-650	Error while setting a page with PCLV portlet
    * ECMS-648	Impossible to download binary with name containing illegal jcr char using the right popup menu
    * ECMS-645	Problem with displaying documents with name containing illegals JCR char in PCLV
    * ECMS-641	Cache & Categories errors : new published content is not considered and old content remains at cache (need to be removed)
    * ECMS-588	WCM: Image can not be displayed in PCLV
    * ECMS-584	WCM: Can not download an uploaded file
    * ECMS-573	DMS: Comment for a document accepts blank value
    * ECMS-542	[WCM] Nothing happen when click on "Abort" button in Add new content form
    * ECMS-523	Dropdown menu display wrong behavior
    * ECMS-516	Cannot unlock node when HttpSession is timeout
    * ECMS-510	Error when delete user from "User and group management"
    * ECMS-509	Application Error Message in Validator
    * ECMS-504	Some issues need backport to trunk (2)
    * ECMS-470	can not use js file when adding in JS folder in FileExplorer
    * ECMS-454	Content portlets (CLV, SCV, ...) should have permission set to "public" by default
    * ECMS-399	Impossible to add content in a Single Content Viewer if user closed the "Content Selector" from Page Edit mode
    * ECMS-394	Portal Page selector FCK plugin does not show in the title field the selected text
    * ECMS-385	Site Explorer should show the "Document Viewer" by default and not the "File View" on Webcontent
    * ECMS-357	can not download file in ContentListView
    * ECMS-344	In Content Selector form, Uploaded file is not saved
    * ECMS-306	FCKEditor - Content gets deleted when saving or fast publishing
    * ECMS-284	link of document on folder which was add exo:taxonomy action is still displayed content when this document was deleted
    * ECMS-282	Can't create document with new template if a field in this template has name to be ' name'
    * ECMS-251	files uploaded from the formgenerator is renamed and has no extension
    * ECMS-250	Check box & radio field are displayed as text field in added template
    * ECMS-213	Server Patch packaging wrong tomcat files assembly
    * ECMS-183	Can't add gadgets into web content
    * ECMS-171	webdav links not correct in private folder
    * ECMS-131	Link not good after rgit click > copy url to clipboard

** Improvement
    * ECMS-1454	Create an assembly with all JBoss related binaries
    * ECMS-1321	Locked node has no visible UI to show it's currently locked by a user
    * ECMS-1129	SE : alert user when changing page is content has changed
    * ECMS-1114	Reorganize examples directory in WCM
    * ECMS-1060	SE : Filter some Actions when editing w/o popup
    * ECMS-1057	SE : Revamp User Preferences
    * ECMS-1055	Taxonomy tree creation dialog must have a finish button
    * ECMS-1014	Create and Add New Icon for each ECMS Portlet
    * ECMS-975	SE : default view after signing in the site explorer
    * ECMS-952	SE : Save preferences permanently
    * ECMS-933	WCM Cache : Allow enable/disable operation from WCM Composer MBean
    * ECMS-899	Move all ECMS template into DAV & CMIS compliant structure
    * ECMS-882	manage icons style of the categories and items
    * ECMS-876	Support external extension in Administration Portlet (fka Site Admin)
    * ECMS-833	Provide a way to contrel the ordering of the content from the back office to the front end (for business user)
    * ECMS-830	Shouldn't show Taxonomy tree in PCLV configuration form
    * ECMS-809	Should use LOG more exactly
    * ECMS-794	Do not print stack trace when view documents
    * ECMS-605	Allow user to change the selected content in the Single Content Viewer portlet
    * ECMS-391	When uploading an image the content type should be set automatically
    * ECMS-332	Restrict to 1 vote by user
    * ECMS-291	Filter with FCK Editor plugin WCMInsertContent
    * ECMS-108	Merge CLV and PCLV as one portlet

** New Feature
    * ECMS-1069	Dashboard for authoring publication
    * ECMS-958	File Explorer Extensions: Create "Document Blocks"
    * ECMS-769	AuthoringPublicationPlugin : get the actual portal container instance instead of the rootcontainer
    * ECMS-495	Upgrade to use CKEditor3
    * ECMS-105	InContext Editing

** Feedback
    * ECMS-1213	IN the advance tab of the webcontent, the texture are too small
    * ECMS-891	Acceptable MymeTypes & selectorParams for UIOneNodePathSelector
    * ECMS-825	Workspace name in content-publishing process

** Documentation
    * ECMS-1461	Ref Guide: Do not use individual names, put the team
    * ECMS-1300	Java Services
    * ECMS-1296	Inside WCM templates
    * ECMS-1295	Configuration
    * ECMS-1294	Preface
    * ECMS-906	Document the change in the developer and administration guide

** Task
    * ECMS-1503	Release ECMS 2.1.0-GA
    * ECMS-1252	Upgrade to use exopackage 1.1.0 and fix the missing dependencies
    * ECMS-1207	remove annoying log statements
    * ECMS-1144	add svn:ignore
    * ECMS-1143	Create branch 2.1.x from trunk
    * ECMS-1120	Add RichText component in Events dialog
    * ECMS-1113	Add Authoring Plugin by default
    * ECMS-1111	Reuse webui ext from commons project
    * ECMS-1110	Use external configuration instead of component in dms-exte-configuration
    * ECMS-1082	Should have a good final name for ear files
    * ECMS-1077	To add ext-cmis module (artifactId is exo-ecms-ext-xcmis-sp) to the default tomcat assemble.
    * ECMS-1075	Update fisheye URL in pom.xml
    * ECMS-1070	Add svn:ignore in xcmis project
    * ECMS-1065	Override gatein.sh in eXo Content
    * ECMS-1064	eXo Content Packaging Automation
    * ECMS-1050	[unplanned] Import all public wiki WCM documentation in the sources in wikbook format (Part 1)
    * ECMS-973	Create a SVN project for XCMIS extension in WCM
    * ECMS-924	Upgrade to JCR Services 1.12.2-GA
    * ECMS-907	Define and test how to migrate from WCM 2.0GA to 2.1
    * ECMS-858	Upgrade to use JCR 1.12.2-CP01
    * ECMS-805	Upgrade to Gatein 3.1
    * ECMS-797	Backport the fix for ordering symlinks
    * ECMS-789	Rollback to use UIFormMultiValueInputSet of GateIn
    * ECMS-785	use kernel 1.1 xsd
    * ECMS-770	Update system properties for server patch
    * ECMS-762	Localization for AuthoringPublicationPlugin
    * ECMS-756	Upgrade to use GateIn 3.1.0-CR01
    * ECMS-679	Umbrella for issues come from Test Campaign UnTC - WCM 2.0.0 GA2 - Bugs
    * ECMS-216	Organize the structure of ECMS on SVN

** Sub-task
    * ECMS-1459	"ECMS-1298 UI Extension"
    * ECMS-1362	"ECMS-1296 Dialog - AddMixin Field"
    * ECMS-1361	"ECMS-1296 Dialog - Action Field"
    * ECMS-1360	"ECMS-1296 Dialog - RadioInput Field"
    * ECMS-1359	"ECMS-1296 Dialog - Checkbox Field"
    * ECMS-1358	"ECMS-1296 Dialog - Interceptor"
    * ECMS-1357	"ECMS-1296 Dialog - Selectbox Field"
    * ECMS-1356	"ECMS-1296 Dialog - Upload Field"
    * ECMS-1355	"ECMS-1296 Dialog - Calendar Field"
    * ECMS-1354	"ECMS-1296 Dialog - Rich Text Field"
    * ECMS-1353	"ECMS-1296 Dialog - Text Area Field"
    * ECMS-1352	"ECMS-1296 Dialog - Hidden Field"
    * ECMS-1351	"ECMS-1296 Dialog - Text Field"
    * ECMS-1345	"ECMS-1295 Extensions configuration"
    * ECMS-1344	"ECMS-1295 Views configuration"
    * ECMS-1343	"ECMS-1295 Templates configuration"
    * ECMS-1342	"ECMS-1295 Nodetypes configuration"
    * ECMS-1341	"ECMS-1295 Taxonomies configuration"
    * ECMS-1340	"ECMS-1295 Deployment configuration"
    * ECMS-1339	"ECMS-1295 Publication configuration"
    * ECMS-1338	"ECMS-1295 Drives configuration"
    * ECMS-1334	"ECMS-1294 Portlet search"
    * ECMS-1333	"ECMS-1294 Portlet Site explorer"
    * ECMS-1332	"ECMS-1294 Portlet Site administration"
    * ECMS-1331	"ECMS-1294 Portlet Fast content creator"
    * ECMS-1330	"ECMS-1294 Portlet Form generator"
    * ECMS-1329	"ECMS-1294 Portlet newsletter"
    * ECMS-1328	"ECMS-1294 Portlet Content list"
    * ECMS-1326	"ECMS-1294 Portlet Content details"
    * ECMS-1236	"ECMS-1235 Update CKEDITOR from version 3.2 to version 3.3.2"
    * ECMS-1076	"ECMS-105 RSS feed support in CLV (by path)"
    * ECMS-1072	"ECMS-899 Create a new viewer for these template"
    * ECMS-1061	"ECMS-973 To change ext-cmis (eXo CMIS storage provider) sources licence."
    * ECMS-934	"ECMS-105 Need the support from UI team for the layout in InContext Editing"
    * ECMS-923	"ECMS-920 Evaluate and document the way we want to work with content preferences in edit mode"
    * ECMS-904	"ECMS-899 Change ""Initializer Service"" to now import items as nt:file"
    * ECMS-903	"ECMS-899 Implements changes in various services ad UI"
    * ECMS-900	"ECMS-899 List all type of template and items concerned with this change"
    * ECMS-895	"ECMS-105 Merge Development in trunk"
    * ECMS-893	"ECMS-876 Create and document a sample extension"
    * ECMS-881	"ECMS-876 manage label in the extension jar"
    * ECMS-879	"ECMS-876 add svn:ignore"
    * ECMS-878	"ECMS-876 move java code to good place"
    * ECMS-877	"ECMS-876 Create mvn project for administration portlet java classes"
    * ECMS-866	"ECMS-105 SE: manage contextual URL"
    * ECMS-865	"ECMS-105 front: add actions to call the file explorer in the correct context"
    * ECMS-864	"ECMS-850 SE: create a basic mode"
    * ECMS-863	"ECMS-105 SE: Edit without popup"
    * ECMS-854	"ECMS-843 ECM-5458: Customize some informations on SendMailScript.groovy"
    * ECMS-751	"ECMS-679 [WCM] should have tool tip for some icon on Search result tab"
    * ECMS-738	"ECMS-679 [WCM] - Can not edit/open a subscription by Admistration when there is only one subscription in a Category"
    * ECMS-737	"ECMS-679 [DMS] Can not edit new added property of node"
    * ECMS-736	"ECMS-679 DMS: Error when select a taxonomy tree that user does not have right"
    * ECMS-724	"ECMS-679 [DMS-IE7] Actions of a node that uploaded don't work when opening it"
    * ECMS-700	"ECMS-679 [WCM] - Error occurs when trying to view picture detail in Content list"
    * ECMS-681	"ECMS-679 TESTVN-789: DMS: Nodes are always sorted in ascending by ""Create date"" or ""Modify date"""
    * ECMS-583	"ECMS-544 Not exact message display"
    * ECMS-364	"ECMS-105 InContext Editing from CN"
    * ECMS-363	"ECMS-105 InContext Editing from CLV/PCLV"
    * ECMS-362	"ECMS-105 InContext Editing from SCV/PCV"
    * ECMS-361	"ECMS-105 Add support for basic mode"
    * ECMS-360	"ECMS-105 Add support for editing without popup"
    * ECMS-141	"ECMS-504 problem import export content"


** Other resources and links
	Company site        http://www.exoplatform.com
	Community JIRA      http://jira.exoplatform.org
	Community site      http://www.exoplatform.org
	
For more documentation and latest updated news, please visit our websites:
	www.exoplatform.com
	blog.exoplatform.org
	
If you have questions, please send a mail to the list exo-ecms@googlegroups.com.

Thank your for using eXo Content product!
The eXo Content team.
