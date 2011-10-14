Summary
	* A node that don't point to a page should not have a link on Navigation portlet
	* CCP Issue: CCP-1110, Product Jira Issue: ECMS-2748.
	* Complexity: N/A


The Proposal

Problem description

What is the problem to fix?
	* A node that don't point to a page should not have a link on Navigation portlet

Fix description

How is the problem fixed?
	* If a node doesn't reference to any page, we set the 'href' attribute of the node to '#' value

Patch information:
	* ECMS-2748.patch

Tests to perform

Reproduction test
	* Edit navigation of ACME site
	* Add a navigation node (test 1) without adding any page to it.
	* Add a new navigation node (test 2) being child of new created above node.
	* Return to the home page and click on the test 1 -> web browser is redirected to "Page not found".

Tests performed at DevLevel
	* Edit navigation of ACME site
	* Add a navigation node (test 1) without adding any page to it.
	* Add a new navigation node (test 2) being child of new created above node.
	* Return to the home page and click on the test 1 -> web browser isn't redirected

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
	* Function or ClassName change: No

Is there a performance risk/cost?
	* No


Validation (PM/Support/QA)

PM Comment:
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
