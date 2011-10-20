Summary
	* Status: Left mouse double-click on a node does not work properly [Chrome only]
	* CCP Issue: CCP-871, Product Jira Issue: ECMS-2118.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Left mouse double-click on a node does not work properly [Chrome only]

Fix description

How is the problem fixed?
	* To avoid the confusion between double click event and drag-drop event, we calculate the time between onmousedown and onmouseup events. If this interval is less than 200 milliseconds, that event is double click. Otherwise, it is drap & drop event.


Patch information:
	* ECMS-2118.patch 

Tests to perform

Reproduction test
	1. In using Chrome, go to *Sites Management*
	2. Left mouse double-click on a node
	3. The dialog to ask for movement of this node appears.

Tests performed at DevLevel
	* cf. above

Tests performed at QA/Support Level
	* cf. above

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
	* N/A

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
