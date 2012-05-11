Summary
Issue title Search results are not correct when searching in "classic" site
CCP Issue:  N/A 
Product Jira Issue: ECMS-3714.
Complexity: Normal
Proposal
 

Problem description
What is the problem to fix?

 Search results are not correct when searching in "classic" site
Fix description
Problem analysis
 Search results in "classic" site contain "parameterizedviewer", it should be changed to "detail" to be correct. Moreover, we need to implement a migrate tool  to migrate legacy data.

How is the problem fixed?
change preference *basePath* of portlet *WCMAdvanceSearchPortlet* from *parameterizedView* to *detail*
create a migration tool to migrate legacy data

Tests to perform
Reproduction test

Steps to reproduce:
Go to classic
Use Quick search with "in Content" option
Enter a key to search (e.g "lorem")
    Result: http://localhost:8080/ecmdemo/classic/parameterizedviewer?content-id=/repository/collaboration/sites%20content/live/classic/ApplicationData/NewsletterApplication/DefaultTemplates/template1 - 2094.0
    The link is not correct: there is no "parameterizedviewer" page in "classic" site. So, we cannot open the content.
    The correct link is: http://localhost:8080/ecmdemo/classic/detail?content-id=/repository/collaboration/sites%20content/live/classic/ApplicationData/NewsletterApplication/DefaultTemplates/template1 - 2094.0
    The problem doesn't happen if we search in acme site.
Tests performed at DevLevel

cf Above
Tests performed at Support Level
*

Tests performed at QA
*

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

Change value of basePath from "parameterizedviewer" to "detail"
Will previous configuration continue to work?

Yes
Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: No
Data (template, node type) migration/upgrade: PortletPreferenceMigrationService
Is there a performance risk/cost?

No
Validation (PM/Support/QA)
PM Comment
PL review: Patch validated
Support Comment
Support review: Patch validated
QA Feedbacks
N/A
