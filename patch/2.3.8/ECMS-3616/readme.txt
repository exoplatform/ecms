Summary
Issue title EmailNotifyListener - Unexpected error NPE
CCP Issue: N/A
Product Jira Issue: ECMS-3616.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?
Exception when watch and unwatch a document 

Fix description
Problem analysis

Current code to get drive's information was wrong.

How is the problem fixed?
Refactor the way to get node's drive

Tests to perform
Reproduction test
Reproduce in ECMS-standalone 
Remove collaboration drive.
Watch a document.
Make some change. Problem: exception in the log.

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
