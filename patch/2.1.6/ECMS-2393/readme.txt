Summary

	* Status: Cannot display document's comments in SCV portlet
	* CCP Issue: N/A, Product Jira Issue: ECMS-2393.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Cannot display document's comments in SCV portlet

Fix description

How is the problem fixed?
	* Load javascript functions which allows user to show and hide comments in rendering SCV portlet

Patch information:
	Patch file: ECMS-2393.patch

Tests to perform

Reproduction test
	* Go to Management sites/acme/documents
	* Add new document using article template,write some comment(s) on this article then publish it
	* Create a page with SCV
	* Edit SCV and insert the created article on it.
	* View created page -> Comments' document doesn't display when click on [Show comment] link

Tests performed at DevLevel
	* See above

Tests performed at QA/Support Level
	* See above

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

Function or ClassName change: No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	* Patch Validated

QA Feedbacks
	*
