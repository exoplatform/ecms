Summary
Show worng list of nodes in workspace after selec workspace that user doesnot permission to view

CCP Issue: N/A
Product Jira Issue: ECMS-3761.
Complexity: N/A
Summary
Proposal
Problem description
Fix description
Tests to perform
Changes in Test Referential
Documentation changes
Configuration changes
Risks and impacts
Validation (PM/Support/QA)
This page should represent the synthesis of information known about the issue fix.
This information will be used to create the general release notes file.
eXo internal information

Impacted Client(s): N/A 


Proposal
 

Problem description
What is the problem to fix?

Login as mary
Go to Content Explorer/Site Management drive
Select 1 node
Open form to add symlink for this node
Click on Add Item icon in Symlink Manager form /Select Target Node form is shown. By default, Collaboration workspace is choosed
Select other workspace. For example, choose: wcm-system workspace / all nodes of this ws are shown.
Select workspace that user doesnot permission to view (e.g: portal-system) /show message: Access denied! You do not have the read permission in this workspace: OK
Click OK on confirm message, the Collaboration workspace is auto selected with nodes of wcm-system workspace : KO
Click on Up level icon: unknown error
SEVERE: Error during the processAction phase
javax.jcr.PathNotFoundException: Can't find path: /
 at org.exoplatform.services.cms.link.impl.NodeFinderImpl.getItem(NodeFinderImpl.java:239)
 at org.exoplatform.services.cms.link.impl.NodeFinderImpl.getItemTarget(NodeFinderImpl.java:164)
 at org.exoplatform.services.cms.link.impl.NodeFinderImpl.getItemGiveTargetSys(NodeFinderImpl.java:90)
 at org.exoplatform.services.cms.link.impl.NodeFinderImpl.getItem(NodeFinderImpl.java:63)
 at org.exoplatform.services.cms.link.impl.NodeFinderImpl.getItem(NodeFinderImpl.java:107)
 at org.exoplatform.ecm.webui.tree.selectone.UIOneNodePathSelector.onChange(UIOneNodePathSelector.java:243)
 at org.exoplatform.ecm.webui.tree.UINodeTreeBuilder.broadcastOnChange(UINodeTreeBuilder.java:313)
 at org.exoplatform.ecm.webui.tree.UINodeTreeBuilder.changeNode(UINodeTreeBuilder.java:301)
 at org.exoplatform.ecm.webui.tree.UINodeTreeBuilder$ChangeNodeActionListener.execute(UINodeTreeBuilder.java:335)
 at org.exoplatform.webui.core.UIComponent.broadcast(UIComponent.java:362)
 at org.exoplatform.webui.core.UITree$ChangeNodeActionListener.execute(UITree.java:395)
Fix description
Problem analysis

The problem of this issue is inconsistent between workspace of UIOneNodePathSelector and UIWorkspaceList. This causes exception when getting nodes of this workspace with the path of another workspace.
How is the problem fixed?

To fix this issue we need filter workspace at beginning to make sure that users is not able to select a workspace which they have no permission.
While fixing this issue, I found that a file with same name (UIWorkspaceList.java) but it's not used. So I have removed this file from our's code.

Tests to perform
Reproduction test

Login as mary
Go to Content Explorer/Site Management drive
Select 1 node
Open form to add symlink for this node
Click on Add Item icon in Symlink Manager form /Select Target Node form is shown. By default, Collaboration workspace is choosed
Select other workspace. For example, choose: wcm-system workspace / all nodes of this ws are shown.
Select workspace that user doesnot permission to view (e.g: portal-system) /show message: Access denied! You do not have the read permission in this workspace: OK
Click OK on confirm message, the Collaboration workspace is auto selected with nodes of wcm-system workspace : KO
Click on Up level icon: unknown error
Tests performed at DevLevel

cf. above
Tests performed at Support Level

cf. above
Tests performed at QA

cf. above
Changes in Test Referential
Changes in SNIFF/FUNC/REG tests

No
Changes in Selenium scripts 

No
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:

No
Configuration changes
Configuration changes:

No
Will previous configuration continue to work?

Yes
Risks and impacts
Can this bug fix have any side effects on current client projects?

N/A
Function or ClassName change

No
Data (template, node type) migration/upgrade

No
Is there a performance risk/cost?

No


Validation (PM/Support/QA)
PM Comment
PL review: Patch validated
Support Comment
Support review: Patch validated
QA Feedbacks
N/A
