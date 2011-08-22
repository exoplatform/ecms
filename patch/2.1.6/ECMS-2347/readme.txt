Summary

	* Status: Lucene's Fuzzy search parameter is not given in PLF
	* CCP Issue:  CCP-987, Product Jira Issue: ECMS-2347.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Lucene's Fuzzy search parameter is not given in PLF

Fix description

How is the problem fixed?
	* Modify the query search to enable/disable Fuzzy Search and customize Fuzzy Search's parameter

Patch information:
	Patch file: ECMS-2347.patch

Tests to perform

Reproduction test
	- Lucene's Fuzzy search in Front Office enable users to search not only exactly a word but also other words being close to the key word.
	- The parameter of Lucene's Fuzzy search is, in defaut, 0.5 and this value is varied between 0 and 1. But in eX, there are no method to customise this value.

Tests performed at DevLevel
	*

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* Yes 

Configuration changes

Configuration changes:
	* N/A

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change

Is there a performance risk/cost?
	* Once fuzzy search is enable, the search will take more time

Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	* Patch Validated

QA Feedbacks
	*
