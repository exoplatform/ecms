Summary
Issue title 
CCP Issue: N/A
Product Jira Issue: ECMS-3653.
Complexity: Normal
Proposal
 

Problem description
What is the problem to fix?

FavoriteService is not secure
Fix description
Problem analysis

 In FavoriteServiceImpl, we use SystemSessionProvider to get and return favorite node for user. That is not good because user can get access to SystemSession through these nodes.
How is the problem fixed?

In FavoriteServiceImpl, we replace SystemSessionProvider by UserSessionProvider to get and return nodes for user. Also change the TestFavoriteService, apply UserSession to test. 
Patch file: ECMS-3653.patch
Tests to perform
Reproduction test

Go to http://localhost:8080/ecmdemo/rest-ecmdemo/favorite/all/repository/collaboration/xyz
    -> add 1 json file named xyz
 {"listFavorite":[]}

Go to http://localhost:8080/ecmdemo/rest-ecmdemo/jcr/repository/collaboration/Users/x___/xy___/xyz as if there's a newly created account.
Tests performed at DevLevel

cf Above
Tests performed at Support Level

...
Tests performed at QA

...
Changes in Test Referential
Changes in SNIFF/FUNC/REG tests

change in TestFavoriteService
Changes in Selenium scripts 

...
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

Function or ClassName change:  No
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
