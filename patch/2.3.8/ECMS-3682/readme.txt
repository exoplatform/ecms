Summary
Issue title Webdav service of cms do not support move or copy folder for cloud-workspaces (multitenancy)
CCP Issue:  N/A 
Product Jira Issue: ECMS-3682.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?
Problem using different repository

Fix description
Problem analysis

The cause of this issue come from getting current repository replace for repo parameter. When we getting repository form current repositoy, we will not take care repo parameter in the url request. So user can access to file system via webdav with a repo contains any string (example: "xyz"). That is the risk.
How is the problem fixed?

So to fix this issue, we don't allow user to access file system while they put a wrong repository name in the url. Therefore, in the patch we will not get current repo in the following functions:
get
propfind
proppatch
move
copy
Patch file: PROD-ID.patch
Tests to perform
Reproduction test

This case supposes that you have an account on cloud-workspaces.com, workspace "exoplatform".

Using webdav client, connect to webdav server via this link: http://exoplatform.cloud-workspaces.com/rest/private/jcr/repository/collaboration/Users/ 
Go to your private folder which you have permission to read/write.
Create a new folder named "folder1". Successful!
Rename folder to "folder2". Unsuccessful, 409 CONFLICT error is thrown.
Copy folder to another. Unsuccessful, 409 CONFLICT error is thrown.
These above unsuccessful actions can be passed if we use the link (replace 'repository' by 'exoplatform') http://exoplatform.cloud-workspaces.com/rest/private/jcr/exoplatform/collaboration/Users/ 
Tests performed at DevLevel

cf.above
Tests performed at Support Level

cf.above
Tests performed at QA

cf.above
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

N/A
Data (template, node type) migration/upgrade

No
Is there a performance risk/cost?

No
Validation (PM/Support/QA)
PM Comment

PL review: Patch validated
Support Comment

Support review: Patch validated
QA Feedbacks

N/A
