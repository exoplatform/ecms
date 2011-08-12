Summary

	* Status: In case of multi pages, page numbers navigation in "Sites Management" is not available until web browser is resized
	* CCP Issue:  CCP-994, Product Jira Issue: ECMS-2363.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* In case of multi pages, page numbers navigation in "Sites Management" is not available until web browser is resized

Fix description

How is the problem fixed?
	* Modify position of pagination

Patch information:
	* Patch file: ECMS-2363.patch

Tests to perform

Reproduction test
	1. Go to Site Explorer > Sites Management
	2. Import a node containing many contents.
	3. Go to Group Editor > Edit Layout and remove the Footer portlet.
	4. Go to Group Editor > Edit Page and remove the Authoring portlet.
	5. Edit portlet Content Explorer. In "Edit Mode" Tab, uncheck "Show Filter Bar".
	6. Page numbers navigation in "Sites Management" is not available until web browser is resized

Tests performed at DevLevel
	*

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change : No

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* PATCH VALIDATED

Support Comment
	* Patch validated

QA Feedbacks
	*
