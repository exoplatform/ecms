Summary

	* Status: PERF : New pagination mode in Search portlet
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2490.
	* Complexity: Hard

h1. The Proposal

Problem description

What is the problem to fix?
	* New pagination mode in Search portlet

Fix description

How is the problem fixed?
	* Add new option for Search Result template configuration, provide more display mode of searching result when there are many found results.
	* Change the template and UIComponents of search, add logic to manipulate and switch between pagination modes

Patch information:
	Patch file: ECMS-2490.patch

Tests to perform

Reproduction test
	* Run WCM, login, go to [http://localhost:8080/ecmdemo/private/acme] and put a word (abc for example) in the simple search box
	* In the result list, there is only one way to display the result: pagination
	* Edit the search portlet, see that we can not choose page mode.


Tests performed at DevLevel
	* cf above

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* Yes

Configuration changes

Configuration changes:
	* No

Will previous configuration continue to work?
	* Yes

h2. Risks and impacts

Can this bug fix have any side effects on current client projects?
	* No

Function or ClassName change
	* No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	*

QA Feedbacks
	*
