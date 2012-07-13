ECMS-3832
More Add Page Edit
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
See [company:Maintenance Issues Actions per Team] for more information.
eXo internal information

Impacted Client(s): N/A 
Summary
Content of default.hmlt is shown on CLV portlet when content selection mode is "by folder" 
CCP Issue:  N/A
Product Jira Issue: ECMS-3832.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?
A document is considered to be a folder when we create a CLV page.

Fix description
Problem analysis
 When we create a document: article, freewebcontent etc., its structure is a folder. However, it is not a normal folder for CLV page so we need to disable to view document as a folder when creating CLV page

How is the problem fixed?
Modify the function isFolder. We check whether the node is a document. If this is the case, return false 

Tests to perform
Reproduction test

Login
Go to content explorer
Create new free layout web content named "web content 1" and do not publish it
Go to acme home page, add new page with CLV portlet
Click edit icon
On content selector form select mode by folder and browse link to folder "web content 1", save this selection
Save your page
Problem: Content of default.htmll is displayed in publish mode
Tests performed at DevLevel

cf. above
Tests performed at Support Level

cf. above
Tests performed at QA

N/A
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

Function or ClassName change: No
Data (template, node type) migration/upgrade: No
Is there a performance risk/cost?

No
Validation (PM/Support/QA)
PM Comment

PL review: Patch validated
Support Comment

Support review: Patch validated
QA Feedbacks

N/A
