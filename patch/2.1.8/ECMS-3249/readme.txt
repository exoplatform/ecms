Summary
	* Status: Content Explorer portlet isn't well displayed with Symblink
	* CCP Issue: CCP-1185, Product Jira Issue: ECMS-3249.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Content Explorer portlet isn't well displayed

Fix description

How is the problem fixed?
	* Check that we only do split nodePathUrl string to get the portletId when the length of nodePathUrl string is greater than 1, and the nodePathUrl string contains "/" character

Patch information:

Patch files: ECMS-3249.patch

Tests to perform

Reproduction test
	* Connect as root and go to the url: http://localhost:8080/ecmdemo/private/acme
	* Edit page then add Content Explorer portlet into homepage
	* Back to homepage then click acme driver on CE portlet -> error

Tests performed at DevLevel
	* Do the same steps as the reproduction test => have no any error.

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
