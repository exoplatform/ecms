Summary
FavoriteService is not secure with administration

CCP Issue:  N/A
Product Jira Issue: ECMS-3800.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?

Login by administration
Go to http://ecms-2.3.7-snapshot.acceptance.exoplatform.org/ecmdemo/rest-ecmdemo/favorite/all/repository/collaboration/xyz:  1 json file named xyz
{"listFavorite":[]}
Go to http://ecms-2.3.7-snapshot.acceptance.exoplatform.org/ecmdemo/rest-ecmdemo/jcr/repository/collaboration/Users/x___/xy___/xyz as if there's a newly created account.

Fix description
Problem analysis

In FavoriteServiceImpl, don't check if favorite node existed or not for user.
How is the problem fixed?

In FavoriteServiceImpl, we more parameter OrganizationService organizationService
Use Organization service to check whether user to add favorite node existed or not
Change the TestFavoriteService, apply OrganizationService
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
