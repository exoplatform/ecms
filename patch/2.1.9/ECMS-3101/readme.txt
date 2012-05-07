Summary
Issue title:Impossible manipulation of a document containing & in its name 
CCP Issue:  CCP-1161 
Product Jira Issue: ECMS-3101.
Complexity: N/A

Impacted Client(s): SPFF and probably all
Proposal
 

Problem description
What is the problem to fix?

Impossible manipulation of a document containing & in its name
Fix description
Problem analysis

'&' is escaped to '%26'
How is the problem fixed?
*
   - Add options=noSanitization for property name to dialog template of nt:file
   - Use URLEncoder.encode  to encode node path  in view template of comment
   - Remove '&' from illegal JCR characters list in Text.escapeIllegalJcrChars
   - Use Text.escape(nodePath,'%',true) in PermlinkActionComponent.java

Tests to perform
Reproduction test

Steps to reproduce:
Create a document containing & in its name on local (EX mac&cheese.doc)
Copy the document in a workspace (EX Collaboration)by webdav:
In File Explorer, the document appears in the right name but we can only download this document. If we try to do any actions (rename, copy, cut, delete), a popup appears (see attachment)and an error appears also in the log:
 ERROR JCRExceptionManager The following error occurs : javax.jcr.PathNotFoundException: Can't find path: /Documents/Live/mac
 WARN UIJCRExplorer The node cannot be found at /Documents/Live/mac into the workspace collaboration
In Webdav: the document appears with an incorrect name ( it considers only characters before & and the document will be without extension (in our case it will be mac) and we can't do any actions also (rename, copy...)
Upload the same document in File Explorer and verify:
In File Explorer: The name of document is correct and we can do all actions
In Webdav: the name of document is also correct but we can only copy it. We can't rename, cut and delete it.
Tests performed at DevLevel

cf. above
Tests performed at Support Level

cf. above
Tests performed at QA
*

Changes in Test Referential
Changes in SNIFF/FUNC/REG tests

None
Changes in Selenium scripts 

None
Documentation changes
Documentation (User/Admin/Dev/Ref) changes:

None
Configuration changes
Configuration changes:
*No

Will previous configuration continue to work?
*Yes

Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: 
- packaging/wcm/webapp/src/main/webapp/WEB-INF/conf/dms-extension/dms/artifacts/templates/file/dialogs/dialog1.gtmpl
- packaging/wcm/webapp/src/main/webapp/WEB-INF/conf/dms-extension/dms/artifacts/templates/comment/view1.gtmpl
- core/services/src/main/java/org/exoplatform/ecm/utils/text/Text.java
- core/webui-explorer/src/main/java/org/exoplatform/ecm/webui/component/explorer/rightclick/manager/PermlinkActionComponent.java
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
