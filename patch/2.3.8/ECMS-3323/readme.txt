Summary
Issue title  Cannot view a web content when html and css have been uploaded
CCP Issue:  N/A
Product Jira Issue: ECMS-3323.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?

Cannot view a web content when html and css have been uploaded
Fix description
Problem analysis

The cause of this issue is the system cannot find path for html and css nodes of web content when displaying it. The root cause related to behavior about OnParentVersion attribute of JCR. 
How is the problem fixed?

We will fix this issue as following:
 Disable manage version and manage publication actions when we accessing child nodes of web content.
 When we upload a file (default.html or default.css) to a web content. We will not add mix:versionable into the uploaded file.
Tests to perform
Reproduction test

Login on intranet as john, then go to Sites Explorer, Sites management
Add a "free layout web content" in intranet/web contents, save it.
Click on the web content, click Upload Files, and upload default.html
Click on the css folder, click Upload Files, and upload default.css
Publish default.css (click on Manage Publication, enroll into Authoring publication, then click on Publish)
Publish on default.html (click on Manage Publication, then click on Publish)
Go back to intranet home, add a Content Details portlet
Edit preferences and select the web content above
Switch to edit mode, the content is draft, click on front publish action
Switch off edit mode >> content disappears and NodeNotFoundException in consle
Tests performed at DevLevel

cf. above
Tests performed at Support Level

cf. above
Tests performed at QA

cf. above
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
