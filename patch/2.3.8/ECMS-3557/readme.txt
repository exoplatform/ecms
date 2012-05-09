Summary
Issue title When clicking "Save" in edit, full page mode the form is saved and open "in normal mode" (not full page)
CCP Issue:  N/A
Product Jira Issue: ECMS-3557.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?
Edit an article in full page mode. Click save. Problem: the edit mode becomes "normal mode". It should stay in full page mode.

Fix description
Problem analysis
We don't save the current screen mode to restore it after ajax request.

How is the problem fixed?
Save the current screen mode, try to restore saved mode after ajax request

Tests to perform
Reproduction test
1- Go in the File Explorer
2- Edit an Article
3- Click in "Full Page" Window
4- Click Save
5- BUG : the form is back in "normal" view (not full page)
We need for better user experience keep the same mode

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
