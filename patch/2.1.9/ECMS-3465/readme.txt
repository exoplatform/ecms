Summary
Issue title Cannot view content of File in CE
CCP Issue: N/A
Product Jira Issue: ECMS-3465.
Complexity: N/A

Impacted Client(s): N/A 


Proposal
 

Problem description
What is the problem to fix?

Login, go to CE, create a new File
Click Save, then close
Issue: display information that "This content is not viewable. Please download it or use WebDAV to access the content", so user cannot view content of file
Fix description
Problem analysis

Cannot view content of File in CE
How is the problem fixed?

If file type is text, not check changing jcr:mimeType

Tests to perform
Reproduction test
* cf. above

Tests performed at DevLevel
* N/A

Tests performed at Support Level
* N/A

Tests performed at QA
*

Changes in Test Referential
Changes in SNIFF/FUNC/REG tests
No
Changes in Selenium scripts 
No
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:None


Configuration changes
Configuration changes:
No

Will previous configuration continue to work?
Yes

Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: 
core/webui-explorer/src/main/java/org/exoplatform/ecm/webui/component/explorer/UIDocumentInfo.java
Data (template, node type) migration/upgrade: 
Is there a performance risk/cost?

No


Validation (PM/Support/QA)
PM Comment
PL review: Patch validated
Support Comment
Support review: Patch validated
QA Feedbacks
N/A
