Summary

	* Status: ContentListViewer doesn't sort contents from categories
	* CCP Issue: CCP-1003, Product Jira Issue: ECMS-2402.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* ContentListViewer doesn't sort contents from categories

Fix description

How is the problem fixed?
	* Broadcast UPDATE_EVENT when one node is updated. This task make all the symlink of that node will be updated too.

Patch information:
	* Patch file: ECMS-2402.patch

Tests to perform

Reproduction test
Case 1
	1. Go to News tab and edit News portlet configuration: Order by Modification date, Descendant
	2. Edit a news in "/acme/categories/acme/" with Content Explorer (e.g Power Pack 1 news)
	3. Go to News tab: check sorted news: not OK

Case 2
	1. Go to News tab and edit News portlet configuration: Order by Publication date, Descendant
	2. Edit and Publish a news in "/acme/categories/acme/" with Content Explorer (e.g Power Pack 1 news)
	3. Go to News tab: check sorted news: not OK


Tests performed at DevLevel
	*

Tests performed at QA/Support Level
	*


Documentation changes

Documentation changes:
	* N/A

Configuration changes

Configuration changes:
	* N/A

Will previous configuration continue to work?
	* N/A


Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: No

Is there a performance risk/cost?
	*

Validation (PM/Support/QA)

PM Comment
	* PATCH VALIDATED

Support Comment
	* Patch validated

QA Feedbacks
	*
