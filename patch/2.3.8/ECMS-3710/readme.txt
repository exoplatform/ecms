Summary
Cannot edit upload document whose name contains accent character 

CCP Issue: N/A
Product Jira Issue: ECMS-3710.
Complexity: N/A

This page should represent the synthesis of information known about the issue fix.
This information will be used to create the general release notes file.
eXo internal information

Impacted Client(s): N/A 
Proposal
 

Problem description
What is the problem to fix?
Login as john
Upload a file whose name contains accent character i.e ááááá ã ã ã óóóóááááá ã ã ã óóóó.txt -> Success
Try to edit it in Content Explorer
After editing, Save or Save and Close > Exception and cannot save


Node not found /sites content/live/acme/documents/%26aacute;%26aacute;%26aacute;%26aacute;%26aacute; %26atilde; %26atilde; %26atilde; %26oacute;%26oacute;%26oacute;%26oacute;%26aacute;%26aacute;%26aacute;%26aacute;%26aacute; %26atilde; %26atilde; %26atilde; %26oacute;%26oacute;%26oacute;%26oacute;.txt
at org.exoplatform.services.jcr.impl.core.NodeImpl.getNode(NodeImpl.java:1025)
at org.exoplatform.services.cms.impl.CmsServiceImpl.storeNode(CmsServiceImpl.java:197)
at org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentForm.saveDocument(UIDocumentForm.java:423)
Fix description
Problem analysis

Using a third party library (antisamy version 1.4) to sanitize input data to avoiding xss problem.  However it generate problem with accent file name.

How is the problem fixed?

Using antisamy version 1.4.5 resolves the problem

Update pom.xml and wcm.packaging.module.js to use antisamy 1.4.5

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
