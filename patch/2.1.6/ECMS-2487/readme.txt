Summary

	* Status: PERF : Filter Optimization
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2487.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Filter Optimization on Content Explorer

Fix description

How is the problem fixed?
	* If there isn't any filter, no need to inspect node list to check that it's passed the filter or not. In this case, simply, we return node list

Patch information:
	Patch file: ECMS-2487.patch

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
	* Function or ClassName change: No

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* PATCH VALIDATED

Support Comment
	*

QA Feedbacks
	*
