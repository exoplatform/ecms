Summary:

	* Status: PERF: Limit Membership queries
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2492.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Method Utils.getMemberships is recalled many times in many methods in the class UIDrivesArea

Fix description

How is the problem fixed?
	* Use an instance's local variable to store result of first time method calling and reuse that variable

Patch information:
	Patch file: ECMS-2492.patch

Tests to perform

Reproduction test
	*

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
	* N/A

Is there a performance risk/cost?
	* No

Function or ClassName change: No


Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	*

QA Feedbacks
	*
