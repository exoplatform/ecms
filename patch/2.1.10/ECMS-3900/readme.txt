ECMS-3900
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
 Right Click Menu doesn't work properly on some views
CCP Issue: N/A
Product Jira Issue: ECMS-3900.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?

When using Icon View or Thumbnails view, user got exception when trying to rename or delete a document node
Fix description
Problem analysis

in the template file of Icon View and Thumbnails View, we got the wrong node path 
How is the problem fixed?

Corrects the node path in Icon View and Thumbnails View template file 
Tests to perform
Reproduction test

Go to Administration/Content Presentation/Manage Drives to apply view Icon View or Thumbnails view to drive such as collaboration
Go to Content Explorer, collaboration drive
Add or upload document
Right click on the document
Choose some action such as rename, delete. Problem: node not found
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
