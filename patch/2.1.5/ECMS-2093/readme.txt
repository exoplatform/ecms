Summary

    * Status: Error with Category navigation portlet after deleted acme category
    * CCP Issue: N/A, Product Jira Issue: ECMS-2093.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
REG_AIO168_WCM_003

Error with Category navigation portlet after deleted acme category

Steps:

    * Go to ACME site -> Select News page.
    * Go to Site Editor ->Edit Page Wizard.
    * Go to step 3
    * Click x icon to delete PCLV portlet (on the right, Category content)
    * Click OK to confirm
      Go to Sites Explorer/Sites Management under /acme/categories delete the acme category.
    * Select News page
    * Go to Site Editor ->Edit Page Wizard.
    * Go to step 3
    * Drag drop Category Content into page
    * Click Edit icon to edit Category Content portlet --> edit form is shown OK
    * Click Edit icon to edit Category Navigation portlet --> Edit form is blank & exception in console
      ?
      10:36:32,780 ERROR [PortletApplicationController] Error while rendering the porlet
      javax.jcr.PathNotFoundException: Node not found /sites content/live/acme/categories/acme
      at org.exoplatform.services.jcr.impl.core.NodeImpl.getNode(NodeImpl.java:946)
      at org.exoplatform.services.wcm.publication.WCMComposerImpl.getViewableContents(WCMComposerImpl.java:243)
      at org.exoplatform.services.wcm.publication.WCMComposerImpl.getContents(WCMComposerImpl.java:204)
      at org.exoplatform.wcm.webui.clv.UICLVFolderMode.getRenderedContentNodes(UICLVFolderMode.java:120)
      at org.exoplatform.wcm.webui.clv.UICLVFolderMode.init(UICLVFolderMode.java:71)
      at org.exoplatform.wcm.webui.clv.UICLVPortlet.processRender(UICLVPortlet.java:249)
      at org.exoplatform.webui.application.portlet.PortletApplication.render(PortletApplication.java:253)
      at org.exoplatform.webui.application.portlet.PortletApplicationController.render(PortletApplicationController.java:113)

Fix description

How is the problem fixed?

    * check null when we get contents by WCMComposer

Patch files:ECMS-2093.patch

Tests to perform

Reproduction test

    * Login as root.
    * Click on "Groups/Site Explorer" on the admin toolbar.
    * Click on the "Site Management" drive.
    * Type "/acme/categories" at the address bar.
    * Delete acme category
    * Goto the News page of the acme portal
    * --> Exception on the server console

Tests performed at DevLevel

    * Do the same tasks like reproduction test => have no Exception is thrown

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:
*No
Configuration changes

Configuration changes:
*No

Will previous configuration continue to work?
*Yes
Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change

Is there a performance risk/cost?
*No
Validation (PM/Support/QA)

PM Comment
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*


