Summary
	* Status: Loose the Simple Search validation's criterion
	* CCP Issue:CCP-1019, Product Jira Issue: ECMS-2452.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Loose the Simple Search validation's criterion

Fix description

How is the problem fixed?
	* Remove unnecessary filters and use SimpleSearchValidator.java to validate data
	* Use try-catch to catch the invalid query

Patch information:
	* Patch files: ECMS-2452.patch

Tests to perform

Reproduction test
	1. In Simple Search form, it defines itself search validator which is different with search validator in advanced search
		* Cannot search with * (which find all documents)
		* Cannot search with "~"
	2. It should loose the simple search validation criterion as advanced search one.
	3. Advanced search raises exception when search criteria is "+" or "-"


Tests performed at DevLevel
	* Do the same step as reproduction test => have no exception

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

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
