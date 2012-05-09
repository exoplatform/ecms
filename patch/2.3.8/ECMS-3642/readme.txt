Summary
Upload Image" from "WCM Content Selector" doesn't work forma custom form 

CCP Issue:  CCP-1027 
Product Jira Issue: ECMS-3642.
Complexity: N/A

Impacted Client(s): REDHAT

Proposal
 

Problem description
What is the problem to fix?
*It's not possible to select images to custom forms that uses the Rich Editor using WCM Content Selector.
Customer gave good steps to reproduce it(it's a SP issue):

start default profile, log in
2. Go to "Form Builder" type any name and description
3. go to "Form Builder" tab
4. click at "Editor" button on the left. A rich editor is placed at the right side
5. Click at rich editor property
6. Change the value of "Advance Options Toolbar" to "CompleteWCM" and save
7. Go to "Content explorer" and navigate to /classic/web contents
8. add a folder name it test
9. go into the folder
10. add content and select the recently created form builder
click at the image select button 
12. click the "Locate in the server" button
13. Go to "General Drives / Sites Management / acme / medias / images / GlobalImages" folder
14. Click at any image, it doesn't select the file and closes the window.
Fix description
Problem analysis

When we creating a Form by Form Builder and contains WYSIWYG component inside it. The id of CKEditor component will be automatically created with format as  "/node/ckeditorxxxx".
This format is invalid for the id of CKEditor, because of it contains slash ("/") character.
How is the problem fixed?

Change the format for id of CKEditor by removing prefix "/node/" when create WYSIWYG component.
Patch file: PROD-ID.patch
Tests to perform
Reproduction test

Login by administrator account (john)
Go to "Form Builder" type any name and description
Go to "Form Builder" tab
Click at "Editor" button on the left. A rich editor is placed at the right side
Click at rich editor property
Change the value of "Advance Options Toolbar" to "CompleteWCM" and save
Go to "Content explorer" and navigate to /classic/web contents
Add a folder name it test
Go into the folder
Add content and select the recently created form builder
Click at the image select button 
Click the "Locate in the server" button
Go to "General Drives / Sites Management / acme / medias / images / GlobalImages" folder
Click at any image, it doesn't select the file and closes the window.
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
Function or ClassName change

No
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
None
