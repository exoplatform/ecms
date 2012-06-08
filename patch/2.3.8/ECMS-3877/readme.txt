Summary
Issue title Unexpected error when editing content with multivalue field
CCP Issue: N/A
Product Jira Issue: ECMS-3877.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?
Cannot edit a content with a multi-value node type

Fix description
Problem analysis
We miss use the function property.getValue() for the multi-value property. So with a multi-value property, we got the wrong result. 

How is the problem fixed?
Using property.getValues() instead of property.getValue() to get content of multi-value property. 

Tests to perform
Reproduction test

1/Create or import a nodetype that contains multivalue properties 
2/Create a new template using this nodetype
3/Create a new content with this template and add more than a value to the multivalue field
4/Click on Save and Close button
5/Click on Edit content
6/Edit some fields and save

An error pop up appears and a log in a Console

13:59:12,923 ERROR [UIDocumentForm] Unexpected error occurrs
javax.jcr.ValueFormatException: The property /test/exo:fg_p_test2 is multi-valued (6.2.4)
at org.exoplatform.services.jcr.impl.core.PropertyImpl.getValue(PropertyImpl.java:152)
at org.exoplatform.services.cms.impl.CmsServiceImpl.processProperty(CmsServiceImpl.java:995)
at org.exoplatform.services.cms.impl.CmsServiceImpl.processNodeRecursively(CmsServiceImpl.java:498)
at org.exoplatform.services.cms.impl.CmsServiceImpl.updateNodeRecursively(CmsServiceImpl.java:346)
at org.exoplatform.services.cms.impl.CmsServiceImpl.storeNode(CmsServiceImpl.java:200)
at org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentForm.saveDocument(UIDocumentForm.java:423)
at org.exoplatform.ecm.webui.component.explorer.popup.actions.UIDocumentForm$SaveAndCloseActionListener.execute(UIDocumentForm.java:684)
at org.exoplatform.webui.event.Event.broadcast(Event.java:89)
at org.exoplatform.webui.core.lifecycle.UIFormLifecycle.processAction(UIFormLifecycle.java:103)
at org.exoplatform.webui.core.lifecycle.UIFormLifecycle.processAction(UIFormLifecycle.java:40)
at org.exoplatform.webui.core.UIComponent.processAction(UIComponent.java:135)
at org.exoplatform.ecm.webui.form.UIDialogForm.processAction(UIDialogForm.java:1589)
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
