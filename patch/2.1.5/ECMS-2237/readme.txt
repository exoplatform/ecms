Summary

    * Status: Nothing happen when click delete path node in Add translation" action
    * CCP Issue: N/A, Product Jira Issue: ECMS-2237.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * Login by mary & go to Site Administration page
    * Choose Content Presentation → Manage View
    * Click Edit icon at the line of admin-view
    * Change permission : *:/platform/web-contributors
    * In the Tabs list: click Collaboration
    * Choose "Add Translation"
    * Click Add tab and Save
    * Edit the drive "platform/users" and select the "admin-view" in the "views" tab
    * Login by James
    * Go to Site Explorer
    * Choose /Platform/Users group drive
    * Choose Documents folder
    * Create "german" folder and go inside it
    * Create an article (title : Mein german article; Language : de (it should be "en" by default if your portal locale is "en")
    * Continue create folder and article for english and french
    * Select to view english article
    * Click on "Add translation" action
    * Browse and find the french article and select it
    * Cont Click on "Add translation" action again
    * Browse and find the french article and select it
      => Show message alert cannot add twice the same language
    * Click to remove path node => Nothing happen and show exception in cygwin

Fix description

How is the problem fixed?

    * In some case the mothod getUIStringInput() of UISymLinkForm class return NULL then system will throw NullPointerException when calling uiSymLinkForm.getUIStringInput(FIELD_NAME).setValue(""). To fix this problem, we will check if getUIStringInput() return UIStringInput of NOT NULL then call setValue("")

Patch files:ECMS-2237.patch

Tests to perform

Reproduction test
 

    * Login by mary & go to Site Administration page
    * Choose Content Presentation → Manage View
    * Click Edit icon at the line of admin-view
    * Change permission : *:/platform/web-contributors
    * In the Tabs list: click Collaboration
    * Choose "Add Translation"
    * Click Add tab and Save
    * Edit the drive "platform/users" and select the "admin-view" in the "views" tab
    * Login by James
    * Go to Site Explorer
    * Choose /Platform/Users group drive
    * Choose Documents folder
    * Create "german" folder and go inside it
    * Create an article (title : Mein german article; Language : de (it should be "en" by default if your portal locale is "en")
    * Continue create folder and article for english and french
    * Select to view english article
    * Click on "Add translation" action
    * Browse and find the french article and select it
    * Cont Click on "Add translation" action again
    * Browse and find the french article and select it
      => Show message alert cannot add twice the same language
    * Click to remove path node => Nothing happen and show exception in cygwin

Tests performed at DevLevel
*cf above

Tests performed at QA/Support Level
*cf above
Documentation changes

Documentation changes:
N/A
Configuration changes

Configuration changes:
N/A

Will previous configuration continue to work?
YES
Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change: N/A

Is there a performance risk/cost?
N/A
Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*

