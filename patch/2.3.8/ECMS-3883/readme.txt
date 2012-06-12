Summary
Issue title [integ-ecms-social] Not the right proportion on preview image on activity when upload document from Site Explorer
CCP Issue:  N/A
Product Jira Issue: ECMS-3883.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?

Go to Site Explorer
2. Upload documents 
3. Go to activity stream. The preview image of the document is displayed not right with proposal.
Fix description
Problem analysis
The code to resize image from document generate some bugs 
 

How is the problem fixed?
Use a third-party software: imgscalr-lib to scale image

Tests to perform
Reproduction test

cf. above
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
