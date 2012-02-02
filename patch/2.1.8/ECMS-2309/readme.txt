Summary
	* Status: Bugs on searching under a category in Sites Explorer
	* CCP Issue: CCP-970, Product Jira Issue: ECMS-2309.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Bugs on searching under a category in Sites Explorer.
		- Bugs on number of result
		- Bugs on searching on taxonomy link
		- Not clear the previous result if a search provides more than 100 result

Fix description

How is the problem fixed?
	* Redo the way to get result list:
		- First, find all the real documents that matched with the keyword
		- Then find all the symlink/taxonomy-link that has target matched with the keyword.
	* Refactor the query for better searching mechanism.
	* Reimplement the way to represent the result list. Instead of filtering the result at the renderring phase, apply filtering before sending it to gtmpl script.

Patch files: ECMS-2309.patch
	
Tests to perform

Reproduction test
	
Case 1
	* Login
	* Go to Content Explorer > Site management
	* Click Saved Search then choose Created Document -> Errors
		- Error 1: Duplicated results on the 1st and 2nd page.
		- Error 2: Wrong in number of displayed results.

Case 2
	* Login
	* Go to Site Explorer > Site Management > * acme* > categories > acme
	* Search with keyword News  -> Error
		- Lack of 1 symlink for New1 on result.
		- Titles should be displayed instead of name

Case 3
	* When we do a search which provides more than 100 results. The result of previous search isn't cleaned.

Tests performed at DevLevel
	* c/f above

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

Function or ClassName change: N/A

Is there a performance risk/cost?
	* Improve performance so much

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	* 

