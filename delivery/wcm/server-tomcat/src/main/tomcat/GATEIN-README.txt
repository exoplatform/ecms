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

Release Notes - exo-ecms - Version wcm-2.0.0-GA

** Bug
    * [ECMS-183] - Can't add gadgets into  web content
    * [ECMS-269] - Bad display font with pdf file when viewing
    * [ECMS-284] - link of document on folder which was add exo:taxonomy action is still displayed content when this document was deleted
    * [ECMS-313] - User is logout when open  a file .pdf
    * [ECMS-335] - DMS gadgets can not display
    * [ECMS-343] - after closing the popup "Welcome to single content viewer" in edit mode, it's not possible to open it again
    * [ECMS-349] - View Image in the other language is not displayed
    * [ECMS-352] - Lose icon of node in Content selector
    * [ECMS-370] - Delete Option
    * [ECMS-371] - Exception while creating Translations
    * [ECMS-376] - Display empty space on sidebar resize
    * [ECMS-388] - Update "How to use this demo ?" content to reflect good user and password
    * [ECMS-400] - Modify Site Explorer home page (default view) to be understandable by the user
    * [ECMS-404] - Error UI when print a document with image attach. see file attach
    * [ECMS-421] - on Firefox, I have an error "Class is undefined" when loading the homepage
    * [ECMS-423] - Can't activate auditing for node
    * [ECMS-432] - Corresponding icon of driver is not shown
    * [ECMS-435] - The first check-box is not uncheck automatically when uncheck on 1 subscription
    * [ECMS-452] - Unknown error when edit CLV without selecting folder path
    * [ECMS-456] - The "Global CSS"of a newly created site is not active by default, when it should be
    * [ECMS-458] - confirm message is not shown when activate version for document
    * [ECMS-459] - broken UI at File View Tab of  File Plan
    * [ECMS-461] - Permission for dialog of template is shown inexactly
    * [ECMS-462] - Prosecc name is double
    * [ECMS-473] - wrong message is shown when rename tag in special case
    * [ECMS-476] - Error while creating content
    * [ECMS-486] - Show CLV configuration form in a new CLV page
    * [ECMS-488] - Edit CLV is invalid in special case
    * [ECMS-490] - Category List Configuration form is not closed after clicking Save
    * [ECMS-491] - Do not shown any node's property created by Form Generator when open form to create document
    * [ECMS-494] - Name of Tab in Single Content Selector is shown inexactly
    * [ECMS-498] - SCV Content not displayed after fresh start
    * [ECMS-501] - Workflow portlets not running on WCM 2.0 CR01
    * [ECMS-502] - Global Sites CSS is removed from then page when deleting a portlet in the site layout
    * [ECMS-505] - Css is broken after editing Site Layout in Acme
    * [ECMS-506] - UI error when view Or print a document with  a picture
    * [ECMS-507] - Lost icon to print /close in print form when print  a page that includes an article
    * [ECMS-511] - Error in Select field
    * [ECMS-512] - Error in dialog Template
    * [ECMS-514] - Rollback FCKEditor
    * [ECMS-515] - Corrupted JCR export in EPP5 only
    * [ECMS-517] - memberships not loaded properly
    * [ECMS-518] - Broken UI when show/hide relation in special case
    * [ECMS-522] - Unable to deploy JBPM process definitions on WCM 2.0 CR 01 (EPP)
    * [ECMS-524] - Can not delete subscription
    * [ECMS-525] - Error when view content of link in SCV
    * [ECMS-526] - Exception in authoringPublication
    * [ECMS-532] - SlideShow cannot view in IE
    * [ECMS-533] - Cannot enroll a predeployed content with Authoring Extension
    * [ECMS-537] - Show error message when trying to select an exist content
    * [ECMS-542] -  [WCM] Nothing happen when click on "Abort" button in Add new content form
    * [ECMS-554] - When add new document, category field does not work correctly if there are also action fields in dialog template
    * [ECMS-555] - can't add a gadget to a webResource
    * [ECMS-598] - 'Null '  is shown in page which reference to an article/free webcontent
    * [ECMS-600] - Unknown error when view permission of taxonomy in special case
    * [ECMS-601] - Can't view picture
    * [ECMS-603] - Site Explorer : left panel doesn't close
    * [ECMS-604] - duplicate folders in wcm site contents
    * [ECMS-608] - Problem with published document
    * [ECMS-610] - Do not show category field when edit Sample node
    * [ECMS-613] - Can not add template by Form Generator
    * [ECMS-614] - Unknown error when remove permisson of a document in acme drive
    * [ECMS-615] - Error while cancel create Content List Viewew by content
    * [ECMS-616] - Can NOT add info for an image in acme
    * [ECMS-617] - CLV configuration form disappears when select folder path
    * [ECMS-619] - Cannot delete Image or reference data in WebContent or sample node
    * [ECMS-623] - Display content-not-found in PCLV portlet
    * [ECMS-624] - Display wrong icon in SE
    * [ECMS-625] - cyclic problem when run WCM on top of EPP5
    * [ECMS-626] - Missing resource bundle for category-view and wcm-view in Site explorer
    * [ECMS-628] - Missing translations in Quick Edit (Inline Edit) FastPublish, SaveDraft, Preferences and Close (or translation does not work)
    * [ECMS-634] - PublicationManager should return content to LifecycleConnector
    * [ECMS-635] - JCRExplorer could change content from WebUI portlets
    * [ECMS-636] - Friendly Service doesn't work with configuration setter
    * [ECMS-637] - LifecycleConnector doesn't return content title
    * [ECMS-638] - Search's results: suggestion always in plural
    * [ECMS-639] - Picture is not shown in manage Publication form
    * [ECMS-640] - In acme, we can't edit permission to a node in taxonomy tree twice
    * [ECMS-643] - searched texts in search results are not highlighted
    * [ECMS-644] - Links in viewMode are not underlined
    * [ECMS-646] - Can't add Action for Document Folder
    * [ECMS-647] - Inherited rights are lost after server restart
    * [ECMS-648] - Impossible to download binary with name containing illegal jcr char using the right popup menu
    * [ECMS-649] - Can not delete new letter
    * [ECMS-650] - Error while setting a page with PCLV portlet
    * [ECMS-651] - In Site Explorer: Wrongly displaying the name of an uploaded file if the name contains special characters.
    * [ECMS-652] - Can't delete Template after edited this template
    * [ECMS-654] - Still show previous results when searching
    * [ECMS-655] - Label categories are not translated in Content Selector
    * [ECMS-657] - Cannot edit and save template
    * [ECMS-659] - SE : Cannot load and merge the bundle
    * [ECMS-661] - Article doesn't allow to link a doc with FCK
    * [ECMS-662] - Cannot find repositoryService variable in TrashFolderScript
    * [ECMS-663] - PCV displays jcr:frozenNode instead of the files names
    * [ECMS-664] - Dark background in FCKEditor
    * [ECMS-665] - Missing resource bundle for WCMInsertContent
    * [ECMS-666] - Translate to Vietnamese for new navigation Events
    * [ECMS-668] - SE : published state isn't translated
    * [ECMS-669] - Localize for label "fieldListTaxonomy" in UIUploadForm
    * [ECMS-686] - User with site edit permission can't create a new page
    * [ECMS-705] - Published content back to draft state after select it

** Improvement
    * [ECMS-425] - Can't name a category with an apostrophe.
    * [ECMS-472] - Add Category based page for event in ECMDEMO
    * [ECMS-534] - Add JMX Support to WCMComposer
    * [ECMS-535] - Add JMX Support to Friendly Service
    * [ECMS-536] - use Kernel profiles to include Friendly extension in WCM
    * [ECMS-541] - DMS: Should add * for mandatory fields in Add/Edit node type form
    * [ECMS-599] - All the things related to extension should be in one jar
    * [ECMS-620] - Separate publication plugins in one jar
    * [ECMS-629] - Apply new style for Classic site

** New Feature
    * [ECMS-261] - Study CKEditor 3.0 adoption
    * [ECMS-264] - Add the ability to Edit and Remove a node type
    * [ECMS-286] - Accessibility : New site template focused on WCA Conformance Level A
    * [ECMS-406] - DMS Document Converter: be able to convert any document in PDF for viewer (but also any type)
    * [ECMS-409] - Improve File Explorer Viewer for various document types : PDF, DOC,FLASH, ...
    * [ECMS-449] - SE : AddLocalizationSymLink UI in the SiteExplorer
    * [ECMS-480] - Support for Friendly URL

** Task
    * [ECMS-129] - Some issues need  backport to trunk
    * [ECMS-358] - duplicated folder in database when create new extension
    * [ECMS-382] - Time Field in content entry template
    * [ECMS-407] - fix the missing translations
    * [ECMS-484] - Acessing a field in Pre Node Save Interceptor
    * [ECMS-519] - Use SYSTEM navigation instead of HIDDEN for pcv detail page
    * [ECMS-520] - Friendly Service shouldn't be active by default
    * [ECMS-521] - use Kernel profiles to include Authoring extension in WCM
    * [ECMS-543] -  [WCM] Should change message when new node type's name contains special character
    * [ECMS-544] - Umbrella for issues come from Test Campaign
    * [ECMS-557] -  [DMS] Always show message in ECM Templates after delete a view in case delete all ECM Templates
    * [ECMS-582] - Performance Test on WCM 2.0
    * [ECMS-618] - Create mvn project for explorer portlet java classes
    * [ECMS-621] - Add default icon for action on action bar
    * [ECMS-630] - Active session leaks detector to check in WCM
    * [ECMS-642] - Deprecate DateTimeClassifyPlugin and comment the test (not usable)
    * [ECMS-673] - change trace logs in authoring cronjobs
    * [ECMS-687] - Release WCM 2.0.0-GA
    * [ECMS-719] - Update Readme file with Install process


** Sub-task
    * [ECMS-471] - Broken style with a popup message & WYSIWYG Field
    * [ECMS-481] - Vote and Comment for Free webcontent layout document are not shown
    * [ECMS-496] - Create an UIComponent for CKEditor
    * [ECMS-538] - DMS: Unknown error when create new drive with " in name
    * [ECMS-539] - DMS - Should set folder default in Allowance to create folder when Edit drive
    * [ECMS-540] -  DMS: Unknown error when search draft node type
    * [ECMS-547] - [WCM] Litter error in [Page Selector] form while select target path
    * [ECMS-548] - IE7: Menu item display under menu item bar
    * [ECMS-550] - [WCM] Should require users put values in fields when configuration for Contact Us form
    * [ECMS-565] -  [WCM] Page's content isn't displayed in Print Preview form
    * [ECMS-568] - [WCM] Show code error in File explorer when update a file same name with existing file
    * [ECMS-570] - Show exception in cygwin when start run Jboss
    * [ECMS-575] - WCM: Label of edit mode is not changed by changing language of site
    * [ECMS-576] - Invalid label add group permission form
    * [ECMS-581] -  [DMS] Can not search when check all type to search
    * [ECMS-591] -  IE7: Don't show "All item" and "By Type" in Site Explorer after click Resize tree button
    * [ECMS-675] - DMS - Unknown error when add new folder in a new drive



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
