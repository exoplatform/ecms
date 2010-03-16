Enterprise Content Management(ECM) > Document Management System(DMS)
Version 3.0.0-Alpha01

You may find it helpful to see the details at wiki place of ECM
http://wiki.exoplatform.org/xwiki/bin/view/ECM/

TABLE OF CONTENTS
---------------------------------------------------
1. What is eXo ECM
2. How to set up eXo ECM
3. Release notes
4. Migration guide


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


2. HOW TO SET UP EXO ECM
---------------------------------------------------
eXo Enterprise Content Management requires the Java 2 Standard Edition Runtime Environment (JRE) or Java Development Kit version 5.x

2.1. Install Java SE 1.5 (Java Development Kit)
Based on JavaEE, our ECM runs currently fine with version 1.5 so if you are using newer version, please download and install this 
version to make ECM works fine. We will support newer version of Java very soon.

2.2. Download eXo Enterprise Content Management version from: http://forge.objectweb.org/projects/exoplatform/

2.3. Unzip that package under a path that does not contain any space (in Windows).

2.4. Open a shell session and go to the bin/ directory that has just been extracted.

2.5. Then run the command :
	Windows:
		eXo.bat run
	Linux, Unix, Mac OS
	chmod u+x *.sh ./eXo run

2.6. Open your web browsers, now eXo ECM can run on FireFox 2 or newer, Internet Explorer 6 or newer 
(we recommend using FireFox 3+ or Internet Explorer 7+ for the best result) 
and navigate to URL: http://localhost:8080/portal

2.7. When the page has been loaded, click "Login" on the top right corner. Specify the username "root" and the password "exo".


3. RELEASE NOTES 
---------------------------------------------------

Release Notes - exo-ecm-dms - Version dms-3.0-RC1

** Bug
    * [ECM-2021] - Error in display File Plan in special case
    * [ECM-2068] - Error with * for required fields in ECM
    * [ECM-2316] - Can not  select permission when edit dialog/view in special case
    * [ECM-2359] - Invalid error message when importing a nodetype without specifying its namespace
    * [ECM-2443] - BaseActionLauncherListener doesn't support anonymous access
    * [ECM-2469] - IE6 - Combo-box appears on top of all others components
    * [ECM-3073] - Can not delete drive
    * [ECM-3090] - Error in displaying 'Content' text-area of Cover-flow in 'Edit ECM Template' form
    * [ECM-3151] - Cannot set value for auto created property(but is not protected)
    * [ECM-3304] - The file size should not be too accurate
    * [ECM-3430] - File Explorer does not render correctly
    * [ECM-3491] - the test on a childNode's existence is not taken into account
    * [ECM-3545] - Do not display Vietnamese language in the search result
    * [ECM-3615] - Error when trying to copy a forlder that contains two nodes with the same name (test, test[2])
    * [ECM-3689] - IE7: Error when close sidebar in File Explorer
    * [ECM-3726] - Hidden node is visible in CB
    * [ECM-3733] - Error in using page iterator in Advanced search result
    * [ECM-3747] - Permission infor is not suitable with selected  version in edit template form
    * [ECM-3758] - some problems  when user without read right try to access document from tag cloud
    * [ECM-3788] - Error in displaying path in Content Browser after do search
    * [ECM-3789] - "exo data" appears in Document templates list when create new document after do search in CB
    * [ECM-3792] - Advanced search: Return blank search result when search location contains space
    * [ECM-3843] - MAC OS: Can not load icon for thumbnail view in right pane
    * [ECM-3885] - Target Path In Add Action for taxonomy form is marked with * but can be blank
    * [ECM-3925] - File explorer does not extend vertically
    * [ECM-3935] - Lose version history when Import node
    * [ECM-4012] - Correct misleading error messages in JCR Explorer import function
    * [ECM-4024] - Error in displaying File Explorer Edit Form with Vista and Mac skin
    * [ECM-4026] - Error in displaying form to add new drive with Vista skin
    * [ECM-4027] - Error in displaying 'Add Query' form with Vista and Mac skin
    * [ECM-4071] - lost all version when do something with node in check-in status
    * [ECM-4080] - Import of a file with its history does not work
    * [ECM-4084] - Improved display in the left side of the File Explorer
    * [ECM-4097] - tag visibility problem
    * [ECM-4098] - [DMS] Cannot do advanced search (search by property, category, type..)
    * [ECM-4102] - Need to click F5 to refresh to see the updated image for Picture on head document in Site Explorer
    * [ECM-4105] - Strange behaviour of Permission
    * [ECM-4109] - Actions on a folder disappear from Action bar after chose Collaboration tab on a document
    * [ECM-4112] - [DMS] Alert message appears below Upload file pop up
    * [ECM-4113] - [DMS] Unknown error when create new ECM/BC templates without name
    * [ECM-4128] - Left panel display issue
    * [ECM-4129] - Default value in selectbox does not work in form dialog metadata
    * [ECM-4135] - [DMS][file explore][admin tab] it can add properties for node when 'Name' field of properties is blank
    * [ECM-4145] - Throw wrong exception in UIDocumentForm
    * [ECM-4147] - http error 404 when retrieving WebDav icons
    * [ECM-4148] - Select category form is not shown for normal user when add new document
    * [ECM-4149] - IE7: Error in displaying document after deleted the comment
    * [ECM-4150] - Change message when delete permission of taxonomy tree
    * [ECM-4153] - Should not allow to view comment node from search result instead of show blank form
    * [ECM-4155] - Error when view multi-languages document from search result
    * [ECM-4163] - Category is not deleted in special case
    * [ECM-4167] - Can not create document in the drive of new repository
    * [ECM-4185] - RGR: DMS Interceptors do not receive the correct "Context"  the "path" is null in 2.5 (was ok in 2.3)
    * [ECM-4186] - Publication "Static and Direct" service change the permission of a node
    * [ECM-4187] - Can not view WebCotent in Content Browser portlet
    * [ECM-4189] - Cannot open jcr nodes containing a URL encoded characters
    * [ECM-4190] - UI Bug in tabs when using advanced search/view content in French (in fact depend fof the width of the tabs)
    * [ECM-4193] - OutOfMemoryError when exporting content
    * [ECM-4194] - Error when view the document in the Content Browser with the imported node (without import/export version history)
    * [ECM-4199] - Content Browser the published node after exprot/import
    * [ECM-4200] - Can not choose uploaded file in document config form of Content Browser
    * [ECM-4202] - Unknown error when add action for taxonomy in special case
    * [ECM-4205] - localization and wording
    * [ECM-4213] - Resize Comment pop-up in File Explorer
    * [ECM-4214] - Potential "race conditions issue" at UIExtensionManagerImpl initialization
    * [ECM-4215] - File Explorer direct access (from URL) does not work properly
    * [ECM-4216] - Display document/uploaded file which added category  in form to add category in special case
    * [ECM-4217] - Do not show content of document after configuring for CB using document
    * [ECM-4235] - RGR:  Modification of the behavior of the ECM addSelectBoxField between 2.5.0 and 2.5.1
    * [ECM-4240] - Unknown error when view deleted document in dms-system workspace in Content Browser
    * [ECM-4241] - IE: Lose stylesheet when view documents in Content Browser 
    * [ECM-4248] - IE7: Error when select page in navigation bar with Vista and Mac skin
    * [ECM-4249] - Error when configuring for FE using Parameterize type without select drive
    * [ECM-4258] - Error when create new document have sam name in other categories
    * [ECM-4276] - Size optimization and Unnecessary blank zone on JCR 
    * [ECM-4280] - Permission of workspace is changed to 'any' while viewing
    * [ECM-4281] - Still display old name of document while viewing in Content Browser
    * [ECM-4282] - Content of releated document is not shown in Content Browser
    * [ECM-4289] - File Explorer Tree View (left pane) does not "link" the various level when the folder name is long (2 lines or more0)
    * [ECM-4290] - When moving file (drag) the file name 'popup' sometimes stays visible.
    * [ECM-4291] - Unknown error when delete attachment of Article or Sample Node document
    * [ECM-4296] - Static and Direct Publication plugin cannot be used by 'standard' user until administrator has used it
    * [ECM-4297] - FolksonomyServiceImpl may let open jcr session for ever
    * [ECM-4332] - Auto init category when add/upload file into document which add category 
    * [ECM-4334] - Category is disappeared after edit document (only Article)
    * [ECM-4335] - Error in showing breadcumb in Content Browser
    * [ECM-4336] - Show message when cut node is being viewed
    * [ECM-4337] - Unknown error when do action with node which added category in specal case
    * [ECM-4338] - Error in selecting breadcrumb in Content Browser in specical case
    * [ECM-4345] - Unknown error when create new folder has space character at the end
    * [ECM-4348] - Right-click menu is not the same while node is being locked 
    * [ECM-4354] - Blanks in drive names are not shown
    * [ECM-4355] - Show image replace content of File document when upload image into File 
    * [ECM-4356] - Can not drag and drop sub of sub node to parent node
    * [ECM-4357] - RepositoryException at first opening of ECMAdmin when there isn't a repository called "repository"
    * [ECM-4359] - 3 scrollbars when using UIListView dsplaying many documents
    * [ECM-4360] - Editing node in UIListView changes page back to first one
    * [ECM-4362] - Clean up chaotic Add Taxonomy Tree dialog
    * [ECM-4363] - Incorrect English in Add Taxonomy Tree Dialog
    * [ECM-4364] - CRON actions are not working 
    * [ECM-4365] - DMS Action does not show the "Cron" information in edit mode of the action
    * [ECM-4379] - Can not do any action when select multi nodes in thumbnails view 
    * [ECM-4382] - Exception when add language for uploaded file
    * [ECM-4391] - Can't find Bonita class PayRaiseUserNameHook on JBoss
    * [ECM-4392] - Some problem with node has special character
    * [ECM-4401] - Portlet ContentBrowser does not update query on front
    * [ECM-4426] - Can't migrate taxonomy from ECM 2.2 to DMS 2.5.1
    * [ECM-4430] - TemplateService.getAllDocumentNodeTypes() is dangerous
    * [ECM-4432] - Show both published and unpublished documents  while configuration for CB to show only published document (with script and query)
    * [ECM-4433] - Exception when edit default taxonomy tree in new repository
    * [ECM-4435] - Concurrent access: simultaneous creation of several documents with the same name
    * [ECM-4442] - Can not add category for document while user has only read permission with taxonomy tree
    * [ECM-4443] - WCM publication cause data loss when restoring previous version of content
    * [ECM-4444] - Exception when select path for Personal drives while configuration for File Explorer
    * [ECM-4445] - All files are disappeared when delete blank form in "Upload" form
    * [ECM-4446] - Can not unlock multi nodes at the same time
    * [ECM-4451] - Problem with empty paths
    * [ECM-4456] - Little mistake in resouce bundle
    * [ECM-4457] - Unknown error when upload multi  file with 1 form is blank
    * [ECM-4461] - Can not add language for Sample node document in special case
    * [ECM-4483] - Problem with the import of the version history for a node type nt: file
    * [ECM-4486] - Unknown error when add action for taxonomy created in workspace with root node type is nt:folder
    * [ECM-4489] - Show both 2 types of folder while editing drive created in workspace with root node type is nt:folder
    * [ECM-4494] - Field to select drive disappears when click on Cancel button while editing File Explorer
    * [ECM-4495] - Show message "Path not found!Maybe, It was removed or path changed! " when select Personal drive 
    * [ECM-4498] - Show all nodes of other drive when view related document 
    * [ECM-4499] - Show comment pop-up when do not select any document in Content Browser
    * [ECM-4502] - Exception when access CB while user does not have permssion 
    * [ECM-4504] - Can create taxonomy tree without select target path when add action for this tree
    * [ECM-4507] - Exception when open form to edit view after enable version
    * [ECM-4521] - NullPointerException when the parsing order is changed
    * [ECM-4525] - Exception when edit File Plan document
    * [ECM-4530] - Can not add new view
    * [ECM-4533] - Can not view thumbnail image of folder
    * [ECM-4535] - Can not upload file if set "Specify a category when uploading a file" 
    * [ECM-4536] - StackOverflowError in org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer after removing site
    * [ECM-4537] - Exception when view metadata of node
    * [ECM-4539] - Exception when view File Plan after editing
    * [ECM-4541] - Exception when view new added language of uploaded file 
    * [ECM-4545] - Still display image after delete in dialogs
    * [ECM-4547] - Exception when add workflow lifecycle for node
    * [ECM-4548] - Exception when back to FE after approving task
    * [ECM-4558] - uicomponent.addCheckboxField doesn't change anything in the JCR
    * [ECM-4570] - JCR Explorer - selection interfeer with scrolling
    * [ECM-4573] - Need to click twice to remove a reference
    * [ECM-4577] - Need icon for document created by new added template
    * [ECM-4583] - Unknown error when click on Save button without select any file to upload
    * [ECM-4591] - Show content of published document after pressing F5  while chosing other node
    * [ECM-4592] - Show duplicate message when do not add category for uploaded file
    * [ECM-4598] - Exception when anonymous create a content on public page
    * [ECM-4601] - Show jcr:frozenNode when view all image files
    * [ECM-4605] - Permission of new workspace of new repository is not shown
    * [ECM-4612] - Show type UIFolderForm.label.nt_folder when create folder in Document Folder
    * [ECM-4618] - Edit Drive generates a 'Workspace path invalid' error.
    * [ECM-4620] - Exception when delete comment of document while node is in check-in status
    * [ECM-4621] - Unknown error when do simple search while viewing 1 symlink node
    * [ECM-4624] - Error in displaying page iterator
    * [ECM-4625] - FE: Error when click on a page No while showing drives list in right pane
    * [ECM-4627] - "root" has full right with * but can not access drives that was assigned for other membership types
    * [ECM-4628] - Error with FE after uploaded an image for a specific drive in ECM Admin
    * [ECM-4629] - FE: Can not jump to chosen path if hit enter after input the path in address bar
    * [ECM-4631] - Problem of Memory leak (OutOfMemoryError.) with DMS 
    * [ECM-4632] - bug : property 'exo:lastModifier' doest not change after rename node
    * [ECM-4636] - "View document" func can not be done except select Collaboration Center drive
    * [ECM-4640] - Error message "msg" on Site Explorer
    * [ECM-4643] - Error in File Explorer when view 1 image file
    * [ECM-4647] - Error with the position of right-click menu when select multi node
    * [ECM-4658] - Error when configuring FileExplorer
    * [ECM-4659] - Content Browser research
    * [ECM-4660] - Redirection to classic portal is hardcoded
    * [ECM-4685] - Find better French translation for DMS drives
    * [ECM-4689] - Cannot use AddTaxonomyActionScript when start server
    * [ECM-4712] - Exception when select new repository
    * [ECM-4714] - Show jcr:frozenNode in Content Browser when view published document
    * [ECM-4715] - Taxonomy import impossible if there are deleted taxonomies
    * [ECM-4720] - Error in org.exoplatform.services.cms.taxonomy.impl.TaxonomyPlugin
    * [ECM-4728] - Some problem with Unlock Management
    * [ECM-4736] - Exception when create document in taxonomy in special case
    * [ECM-4738] - Cannot edit the node with sibling.
    * [ECM-4741] - Can not execute right click action of node in Icon View
    * [ECM-4742] - Exception with lock management component when create new repository
    * [ECM-4750] - Block Site Explorer when upload a file with name including special characters
    * [ECM-4754] - Can not delete node Trash.lnk
    * [ECM-4755] - Can not delete child node of symlink node
    * [ECM-4759] - Workflow :Error when publishing an article in Admin Workflow Management
    * [ECM-4779] - Admin toolbar disapear when using File Explorer and ECM Admin
    * [ECM-4798] - Do not show pop-up menu when right click on multi node
    * [ECM-4813] - File name accepts special characters (like @%#^$^$^#$^^#$^) 
    * [ECM-4818] - Unknown error when add action(not exo:taxonomyAction) for taxonomy tree
    * [ECM-4819] - Still display content of dialog/view/tab after deleting
    * [ECM-4820] - The check-box "Set as DMS System workspace" is not checked
    * [ECM-4821] - Page iterator of Manage Unlock is not working
    * [ECM-4825] - No Management  for exception  on service "org.exoplatform.services.deployment.ContentInitializerService"
    * [ECM-4838] - Data of Folksonomy is not shown
    * [ECM-4840] - Too much unwanted moving nodes in FileExplorer
    * [ECM-4856] - Display two drive has the same name when select multi group permssion for drive
    * [ECM-4871] - Exception when go to FE after creating drive for new repository
    * [ECM-4872] - Unknown error when create new repostiory after deleting 1 workspace
    * [ECM-4878] - Show message "Repository error " when copy/ paste a node in root path
    * [ECM-4879] - Exception when copy/paste 1 node in the same path when node's name has number character at the first
    * [ECM-4880] - Do not show icon to delete attachement of Article document
    * [ECM-4916] - Can not drag and drop node to Document Folder
    * [ECM-4917] - Can not view content of related document (of  Article and Sample Node) after deleting
    * [ECM-4921] - Show message when delete all item in Trash in special case
    * [ECM-4922] - Category is disappeared after restore document
    * [ECM-4959] - NPE while accessing File Explorer in Parameterized mode
    * [ECM-4961] - Print Mode: exo:article template is not printable ( really ugly)
    * [ECM-4964] - NodeItemLinkAware.getDepth() fails
    * [ECM-4973] - unknown error when create Repository in some special cases
    * [ECM-4983] - IE7:Broken UI when go to File Explorer
    * [ECM-4990] - can't drag&drop document (except for  file Plan) when it is viewing
    * [ECM-4992] - Show white blank in bar resizing 
    * [ECM-5049] - don't access File explore in special case
    * [ECM-5062] - Add action :  some options of lifecycle field were chosen but they are not Marked after Saving while there is invalid field 
    * [ECM-5068] - FileExplorer in mode "parameterized", dosen't work after logout /login
    * [ECM-5104] - there are some error messages when go to File Explorer
    * [ECM-5151] - File Explorer portlet show nothing after editting  in special case 





** Improvement
    * [ECM-255] - The admins should be able to unlock a node
    * [ECM-1152] - improve the typing of tags
    * [ECM-1974] - Translations for taxonomy
    * [ECM-2548] - Allow user can create other folder type when add folder.
    * [ECM-2562] - In File Explorer: allow user to rezie column and rearrange them
    * [ECM-2564] - File Explorer: do not shorten the text (filename) when it is not necessary
    * [ECM-2570] - Ability to separate keys from values in the select box and Ability to translate the values
    * [ECM-2842] - Ability to have a tag cloud for a given scope
    * [ECM-2982] - Have an administration panel for locks
    * [ECM-2984] - Remove hard coded references to /portal from org.exoplatform.ecm.connector.fckeditor.FCKUtils
    * [ECM-2985] - Remove hard coded references to /portal from web\ecmportal\src\main\webapp\WEB-INF\conf\script\groovy\SkinConfigScript.groovy
    * [ECM-3056] - Use spellchecker suggestions
    * [ECM-3210] - Allow to upload several files at a time
    * [ECM-3322] - Site Explorer Media Library
    * [ECM-3328] - Fix the performance issue in the PublicationGetDocumentRESTService
    * [ECM-3416] - Allow to create script actions or business process action manageable by the JCR ObservationManager
    * [ECM-3426] - Allow to translate a drive name
    * [ECM-3669] - Set a default value to a multi-value field in a dialog form
    * [ECM-3904] - Upload multiple files via FIleExplorer interface
    * [ECM-3927] - Add Multi Upload support in the File Explorer
    * [ECM-4000] - Do not force user to implement actions in template
    * [ECM-4006] - Can we change the target of the link webdav on the preview page
    * [ECM-4020] - Import All DMS Portlet by default in the Application Registry
    * [ECM-4029] - IMP (SC) : Updated metadata of a locked document 
    * [ECM-4043] - Use mime type application/rss+xml for RSS feeds
    * [ECM-4075] - Use the UI Extension Framework to display the right-click menu
    * [ECM-4116] - Upload of a file with special characters like " ' " in filename is not supported
    * [ECM-4182] - If we allow a node nt:unstructured can be added mix:i18n, we should allow add language for it.
    * [ECM-4201] - localize error message
    * [ECM-4204] - localize words on a roll list
    * [ECM-4206] - Should show label of input for validator popup message instead of name of input.
    * [ECM-4278] - No autoscroll on the scrollbar in the frame "tree view" of FileExplorer where i drap a node (drap&drop)
    * [ECM-4287] - Refactor viewers and add icons to support users zoom in and zoom out
    * [ECM-4293] - The lock token must be stored in a dedicated ExoCache to be able to replicate them over a cluster
    * [ECM-4294] - The LockManagerListener should be an Exo Listener instead of being an HttpSessionListener
    * [ECM-4298] - Display first page of PDF file as a thumbnail 
    * [ECM-4324] - File Explorer toolbar contains lot of spaces between button (especially in FR language)
    * [ECM-4325] - Support execute action with field nodeTypes and isDeep
    * [ECM-4361] - Ugly permission column in taxonomy table
    * [ECM-4398] - Change error message "Invalid characters found in the file name"
    * [ECM-4402] - Improve error message handling when storing nodes
    * [ECM-4431] - Support auto upload when choose the file from computer
    * [ECM-4552] - Translate :JCR: "Are you sure want to move?" in french version
    * [ECM-4555] - Clarification of translation
    * [ECM-4599] - Support to see exo:lastModifier when modify node
    * [ECM-4626] - Improve drives selector which used by edit mode of File explorer
    * [ECM-4653] - Update UI for SlideShow and Media player
    * [ECM-4655] - Allows create a category (exo:category) inside an existing one instead of creating nt:folders
    * [ECM-4656] - Improve FolksonomyService and FavouriteService to reuse symlink feature
    * [ECM-4802] - Should be support trigger action with specific nodetypes
    * [ECM-4811] - Remove redundant code in UIDialogForm
    * [ECM-4868] - Check to remove bussiness process space out of jcr:system
    * [ECM-4870] - Should remove the scroll on Side bar of File Explorer portlet
    * [ECM-4981] - Lookup nodetype in manage nodetype function(ECM Admin) and Manage Action (FE)

** New Feature
    * [ECM-1965] - Tags management
    * [ECM-2146] - Need to fill-in default view and edit templates
    * [ECM-3318] - siteexplorer visual ergonomy (step 2)
    * [ECM-3320] - Ability to add tags by user
    * [ECM-3922] - Share Group Space
    * [ECM-4068] - Add a "Permlink" button in the File Explorer in order to allow to share the direct access URL to a given document with others
    * [ECM-4076] - Convert the nt:file view template to a fully dynamic viewer 
    * [ECM-4277] - Alert article being edited by another user


** Task
    * [ECM-3910] - Ensure that the main features of DMS work in a DMS cluster
    * [ECM-4219] - Search and fix all the potential memory leaks in the cms services
    * [ECM-4239] - Change confirm message when delete permission of taxonomy tree
    * [ECM-4339] - The init parameter "ldap.userDN.key" is missing in the file activedirectory-configuration.xml
    * [ECM-4447] - Upgrade to Portal 2.5.6
    * [ECM-4510] - Remove component ecm out of trunk version
    * [ECM-4862] - Check and fix all problems of  unit test in component/cms to ensure our test still working well
    * [ECM-5032] - improve functionality of Trash
    * [ECM-5098] - Allow enabling & disabling drag&drop node actions in FE
    * [ECM-5162] - File Explorer, ECM Administrator,Content browser,Fast content creator Portlet  don't work after uploading a file


** Other resources and links
	Company site        http://www.exoplatform.com
	Community JIRA      http://jira.exoplatform.org
	Comminity site      http://www.exoplatform.org
	Developers wiki     http://wiki.exoplatform.org
	Documentation       http://docs.exoplatform.org 


4. MIGRATION GUIDE
---------------------------------------------------

DMS can be reached at:

   Web site: http://www.exoplatform.com
						 http://www.exoplatform.vn
   	 E-mail: exoplatform@ow2.org
						 exo-ecm@ow2.org
						

Copyright (C) 2003-2007 eXo Platform SAS.

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