Summary

	* Status: PERF : Private Cache
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2345.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Improving private cache

Fix description

How is the problem fixed?
	* Disable WCMComposer cache(private cache)
	* Use PortletFutureCache (markup cache) to cache the response basing on the request
	* Using ACL SessionProvider to get data for display in markup cache
	* Allow user to choose if he uses markup cache in the configuration of SCV and CLV

Patch information:
	Patch file: ECMS-2345.patch


Tests to perform

Reproduction test
	*

Tests performed at DevLevel
	* Using Jmeter to compare the performance between case of having private cache and case without private cache

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
	* Patch Validated

Support Comment
	*

QA Feedbacks
	*
