Summary
	* Status: Exception when go to News page after delete acme tree
	* CCP Issue: N/A, Product Jira Issue: ECMS-2753.
	* Complexity: Normal

The Proposal

Problem description

What is the problem to fix?
	* Exception when go to News page after delete acme tree

Fix description

How is the problem fixed?
	* Check that NodeIterator is null when fetching node or not. If it's null, no need to display it in WCMComposerImpl class

Patch information:
	Patch files: ECMS-2753.patch

Tests to perform

Reproduction test
	1. Login as admin
	2. Go to Site Explorer -> Sites Management under /acme/categories delete the acme category
	3. Select News page in acme site --> shown exception

Tests performed at DevLevel
	* cf above

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
	*Function or ClassName change: No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
