Summary

    * Status: Cannot go to Space Document after creating a new Space
    * CCP Issue: CCP-1032, Product Jira Issue: ECMS-2660.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * Cannot go to Space Document after create new Space

Fix description

How is the problem fixed?

    * Clear the cache for group drives
    * Retrieve new user roles when a new group is created.

Patch file: ECMS-2660.patch

Tests to perform

Reproduction test
Side effect of ECMS-2493
To reproduce in PLF

   1. Create new Space "testspace"
   2. Go to "testspace" -> "Document": but Private drive will be displayed instead of "/spaces/testspace"

To reproduce in ECMS standalone:

   1. Access to User & Group management portlet
   2. Create new group
   3. Access to Content Explorer portlet
   4. Cannot see the new group drive.

Tests performed at DevLevel
Case 1:

   1. Create new spaces
   2. Go to space's Documents ---> Go to correct space's group drives
   3. Invite some people to space
   4. Login by invited user ---> Able to access space's Documents

Case 2:

   1. Access to Group & User management portlet
   2. Create new a group
   3. Go to Content Explorer portlet ----> See the new group drives

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:
* No
Configuration changes

Configuration changes:
* Yes

Will previous configuration continue to work?
* No, you have to change something in previous configuration to make it work.
Risks and impacts

Can this bug fix have any side effects on current client projects?

    * N/A
    * The changed classes: ManageDriveService.java, ManageDriveServiceImpl.java, UIDrivesArea, core-services-configuration.xml. Added new class NewGroupEventListener.java

Is there a performance risk/cost?
* No
Validation (PM/Support/QA)

PM Comment
* Patch approved.

Support Comment
*

QA Feedbacks
*
