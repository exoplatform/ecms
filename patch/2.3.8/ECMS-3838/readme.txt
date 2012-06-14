Summary
CCP Issue:  N/A
Product Jira Issue: ECMS-3838.
Complexity: Normal
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
According to Grégory Sébert (SPFF consultant):

getLockedNodeList() method with around 3000 nodes take around 1min to execute
{{code}}
Then, we think that the result shoud be paginated and don't load all of the nodes.
Fix description
How is the problem fixed?

Use paginator and query only part of all locked nodes to display, not all of them. When reaching to the last page, we query next part of the locked nodes to display.
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
