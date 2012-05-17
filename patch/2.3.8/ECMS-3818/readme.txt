Summary
Issue title CLONE - Problem when copying a large files
CCP Issue:  N/A
Product Jira Issue: ECMS-3818.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?
the value of current Clipboard was not reset after the second copy

Fix description
Problem analysis
The clipboard is not clear before the second copy

How is the problem fixed?
Clear the clipboard before copy something
 

Tests to perform
Reproduction test

In site explorer,create a folder1 and try to upload 2 files: one file has a size of 120 MB, and one file whose size is small (less than a few kb).
2. create folder2 under site explore.
3. Select the big file, copy and paste to the folder2.
4. Select the small file, copy and paste to the folder2
5. the big file is copied.  NOK
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
*No

Will previous configuration continue to work?
*

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
