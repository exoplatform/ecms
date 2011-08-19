Summary

	* Status: PERF : Dynamic Pagination in CLV
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2489.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Dynamic Pagination in CLV

Fix description

How is the problem fixed?
	* Make use of new feature in JCR query (set offset and limit of query) to get some appropriate contents but not all in CLV. When go to a specific page in a CLV, just get contents of this page by the mechanism above. Do not get all contents in CLV at startup.

Patch information:
	Patch file: ECMS-2489.patch

Tests to perform

Reproduction test
	*

Tests performed at DevLevel
	* cf Above

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
	* No

Function or ClassName change : No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	*

QA Feedbacks
	*
