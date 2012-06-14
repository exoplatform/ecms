Summary
Issue title 
CCP Issue:  N/A
Product Jira Issue: ECMS-3577.
Complexity: Hard
Proposal
 

Problem description
What is the problem to fix?
Data migration does not work from ECMS 2.3.5 to ECMS 2.3.6

Fix description
How is the problem fixed?

Remove wrong statement updateDocumentsTemplate in method TemplateServiceImpl.addTemplate()
Tests to perform
Reproduction test

1- Copy $ecms-2.3.5-tomcat/gatein/data to $ecms-2.3.6-tomcat/gatein/ and run the server (ecms-2.3.6)
2- Go to ecmdemo/acme page. UI problem
3- Go to ecmdemo/classic page. UI problem

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
Data (template, node type) migration/upgrade: exo-ecms-upgrade-templates
Is there a performance risk/cost?

No
Validation (PM/Support/QA)
PM Comment

PL review: Patch validated
Support Comment

Support review: Patch validated
QA Feedbacks

N/A
