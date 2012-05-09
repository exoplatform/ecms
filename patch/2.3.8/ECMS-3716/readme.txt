Summary
Issue title eXo Javascript is conflicting with some JQuery extensions
CCP Issue: N/A
Product Jira Issue: ECMS-3716.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?
Some script javascript is conflict with jquery extensions.

Fix description
Problem analysis

eXo Javascript is conflicting with some JQuery extensions 
How is the problem fixed?

 Remove unused  javascript variable "currentContext" (and its comment), use eXo.env.portal.context instead
 
Tests to perform
Reproduction test
Go to: https://jira.exoplatform.org/secure/attachment/34098/exo-store-assets-portlet.war to download this file
Add file into webapp folder for tomcat or server/default/deploy in Jboss
Start server
Login by root
Go to Application Registry page
Import all application
Add new page. Add Light Box Tester portlet. Save
Click on one of 2 images: there isn't any visual effect as expected.

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
