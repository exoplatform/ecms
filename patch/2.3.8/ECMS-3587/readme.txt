Summary
Issue title NPE when adding property "cleanupPublication" on Unstructured content
CCP Issue: N/A
Product Jira Issue: ECMS-3587.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?
 Adding the property cleanupPublication to the Unstructured content (metro.pdf+offices.jpg+conditions.doc ). However, these contents are not in published state in CE.

Fix description
Problem analysis

cleaupPublication work on PLF but not on ECMS because:
On PLF: imported xml nodes under xml format such as: metro.pdf, offices.jpg, conditions.doc already have publication information
On ECMS: above imported xml nodes have not publication information

How is the problem fixed?

add these nodes directly to the PublicationDeploymentPlugin so these contents are published at deployment time

Tests to perform
Reproduction test

Go to CE / acme/ document
2. the status of the  documents in this folder is empty
it must be in published status

Tests performed at DevLevel

cf. above
Tests performed at Support Level
cf.above
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
