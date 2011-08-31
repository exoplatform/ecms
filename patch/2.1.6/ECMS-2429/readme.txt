Summary

	* Status: On startup, lucene find more than one item with same uuid
	* CCP Issue: N/A, Product Jira Issue: ECMS-2429.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* On startup, lucene find more than one item with same uuid

Fix description

How is the problem fixed?
	* Exclude the add mixin and save session.

Patch information:
	Patch file: ECMS-2429.patch

Tests to perform

Reproduction test
	* On startup, lucene find more than one item with same uuid.

Tests performed at DevLevel
	* JUnit Tests

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* Nothing

Configuration changes

Configuration changes:
	* Nothing

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	*

Support Comment
	*

QA Feedbacks
	*
