Summary
	* Status: "Content explorer" portlet does not work when added in level 3 or more in site navigation
	* CCP Issue: CCP-1134, Product Jira Issue: ECMS-2902.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* "Content explorer" portlet does not work if be added in level 3 or more in Site Navigation

Fix description

How is the problem fixed?
	* Change the way to get the node path in Content Explorer portlet: get from the request parameter named "path"

Patch files: ECMS-2902.patch

Tests to perform

Reproduction test
	* Connect to acme site as root.
	* Add new page "level1"
	* Add new page "level2" children of "level1"
	* Add new page "level3" children of "level2"
	* Drag the "content explorer" portlet into the new page "level3"
	* save ==>The portlet contents / content explorer is corrupted.

Tests performed at DevLevel
	* Do the same step as reproduction test => the content explorer in "level3" page is showed

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	*No

Configuration changes

Configuration changes:
	*No

Will previous configuration continue to work?
	*Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

