Summary
	* Status: Node Type Selector form is not shown at the second time
	* CCP Issue: N/A, Product Jira Issue: ECMS-2756.
	* Complexity: N/A

The Proposal

Problem description
	
What is the problem to fix?
	* Node Type Selector form is not shown at the second time

Fix description

How is the problem fixed?
	* Avoid caching portlet in Edit Mode.

Patch information:
	* Patch files: ECMS-2756.patch

Tests to perform
	1. On Content Explorer, create some Article documents
	2. Create new page and add Content Detail portlet into this page
	3. Edit this portlet
	4. Click on Select path icon -> Select content form is shown
	5. Select Content Search form tab
	6. Choose Document type and click on Add document type icon -> Node Type Selector form
	7. Choose exo:article and click Save -> Article documents created above are listed in Search Result tab
	8. Choose 1 article document -> document's name(title)is shown in Content Path field
	9. Click on Select path icon again -> Select content form is shown
	10. Select Content Search form tab
	11. Choose Document type and click on Add document type icon -> Node Type Selector form is not shown

Tests performed at DevLevel
	*

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* N/A

Configuration changes

Configuration changes:
	* N/A

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
