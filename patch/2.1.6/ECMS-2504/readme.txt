Summary

	* Status: corrupted link in the WYSIWYG field
	* CCP Issue: CCP-1029, Product Jira Issue: ECMS-2504.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Corrupted link in the WYSIWYG field

Fix description

How is the problem fixed?
	1. Remove javascript
	2. Remove <script>
	3. Remove eval javascript function
	4. Replace apostrophe character by #39;

Patch information:
	Patch file: ECMS-2504.patch

Tests to perform

Reproduction test
	1. Log in the ecmdemo portal.
	2. Go to Site Explorer.
	3. Create a new article.
	4. Switch the WYSIWYG to source mode
	5. Paste in the source: <a href="www.google.com">performance on red</a>
	6. Switch out of source mode -> links exist.
	7. Save as draft -> links disappear.


Tests performed at DevLevel
	* cf above.

Tests performed at QA/Support Level
	* cf above

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

Function or ClassName change
	* No

Is there a performance risk cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* PATCH VALIDATED

Support Comment
	* Patch validated

QA Feedbacks
	*
