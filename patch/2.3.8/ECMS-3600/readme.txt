Summary
UI bug on creating new TaxonomyTree
CCP Issue: N/A
Product Jira Issue: ECMS-3600.
Complexity: N/A
Proposal
 

Problem description
What is the problem to fix?
Go to Administration/Manage Categories
Create new or edit 1 taxonomy tree
At the step to add new/edit action for taxonomy tree, click on the icon to select node type
Node Type Selector form is shown
Actual result: UI error

Fix description
Problem analysis

Wrong css of HTML component, wrong usage of div element
The icon and the boolean value was add to different div tag 
How is the problem fixed?

Fix align css, change div element to span element
Add the icon and the boolean value to the same <div> tag.
File: 
 core/webui/src/main/resources/groovy/ecm/webui/nodetype/selector/UINodeTypeSelector.gtmpl

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
