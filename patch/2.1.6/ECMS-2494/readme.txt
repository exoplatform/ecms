Summary

	* Status: PERF: Non recursive navigation in CLV
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2494.
	* Complexity: Normal

The Proposal

Problem description

What is the problem to fix?
	* PERF: Non recursive navigation in CLV

Fix description

How is the problem fixed?
	* In Groovy Template for Category tree, display only one level of child node

Patch information:
	Patch file: ECMS-2494.patch

Tests to perform

Reproduction test
	* Run wcm, login and go to [http://localhost:8080/ecmdemo/private/acme/news]
	* See that the CategoryTree portlet in the left panel shows child nodes in the recursive hierarchy style

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

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* No

Function or ClassName change
	* No

*Is there a performance risk/cost?*
	* No

h1. Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	*

QA Feedbacks
	*
