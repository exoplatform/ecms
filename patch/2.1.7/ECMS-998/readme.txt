Summary
	* Status: Suggestion in search portlet is a link
	* CCP Issue: CCP-852, Product Jira Issue: ECMS-998.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Bug with the suggestion for search keyword 

Fix description

How is the problem fixed?
	* We only get and display suggestion search when have no search result. The suggestion search is created from WCMPaginatedQueryResult object via getSpellSuggestion() function. It isn't a link and can get a null value. If there is no suggestion, no proposal isn't displayed.

Patch information:
	* Patch files: ECMS-998.patch

Tests to perform

Reproduction test
	* Login
	* Input search keyword in simple search box & hit enter. Return search result.
	* Input search keyword in search box (in search portlet) & click Search button. Return search result with suggestion is a link => Error

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
