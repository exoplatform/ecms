Summary
Pay raise request: accept character and negative number in Amount field

CCP Issue:  N/A
Product Jira Issue: ECMS-3771.
Complexity: N/A

Impacted Client(s): N/A 


Proposal
 

Problem description
What is the problem to fix?

Allow inputting character and negative number for Amount field of pay raise request form
Fix description
Problem analysis

Inputting character and negative number are not acceptable for Amount field of pay raise request form
How is the problem fixed?

Not allow to input character and negative number in Amount field by add validator PositiveNumberValidator for Amount field
Tests to perform
Reproduction test

 Login
Go to Business process controller
Select BP Definition Controller tab
Click on Manage start to create new pay raise
Input character or negative number and click save
Save successfully
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
*Yes

Risks and impacts
Can this bug fix have any side effects on current client projects?

Function or ClassName change: 
ext/workflow/bp-jbpm-payraise/src/main/conf/forms.xml
ext/workflow/facade-jbpm/src/main/java/org/exoplatform/services/workflow/impl/jbpm/FormImpl.java
ext/workflow/webui/src/main/java/org/exoplatform/workflow/webui/component/controller/UITask.java
ext/workflow/webui/src/main/java/org/exoplatform/workflow/webui/component/validator/PositiveNumberValidator.java
ext/workflow/portlet-administration/src/main/webapp/WEB-INF/classes/locale/portlet/workflow/WorkflowControllerPortlet_ar.xml
ext/workflow/portlet-administration/src/main/webapp/WEB-INF/classes/locale/portlet/workflow/WorkflowControllerPortlet_de.xml
ext/workflow/portlet-administration/src/main/webapp/WEB-INF/classes/locale/portlet/workflow/WorkflowControllerPortlet_en.xml
ext/workflow/portlet-administration/src/main/webapp/WEB-INF/classes/locale/portlet/workflow/WorkflowControllerPortlet_fr.xml
ext/workflow/portlet-administration/src/main/webapp/WEB-INF/classes/locale/portlet/workflow/WorkflowControllerPortlet_it.xml
ext/workflow/portlet-administration/src/main/webapp/WEB-INF/classes/locale/portlet/workflow/WorkflowControllerPortlet_nl.xml
ext/workflow/portlet-administration/src/main/webapp/WEB-INF/classes/locale/portlet/workflow/WorkflowControllerPortlet_pt_BR.properties
ext/workflow/portlet-administration/src/main/webapp/WEB-INF/classes/locale/portlet/workflow/WorkflowControllerPortlet_vi.xml
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
