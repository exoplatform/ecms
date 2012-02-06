Summary
	* Status: UI bug in FileExplorer Edit Form
	* CCP Issue: N/A, Product Jira Issue: ECMS-3219.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	- Displaying options aren't still aligned on FileExplorer Edit Form

Fix description

How is the problem fixed?
	* In the edit form of File Explorer, We have two elements which aren't still aligned. They are DriveNameInput and SelectNodePath elements. The cause is style sheet of UIFormGrid is wrong. So to fix this issue, We have to modify the style sheet for them as following:
		-Change the width of UIFormGrid element from auto into 100% in order that the width each element on edit form gets the one of edit form.
		- Change the style sheet for td tag and FieldLabel of UIFormGrid. In stead of alignment on each element, we use alignment on table which contains elements.

Patch information:
	* Patch files: ECMS-3219.patch

Tests to perform

Reproduction test
	* The options aren't still aligned on FileExplorer Edit Form

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
	* Patch Validated

Support Comment
	* Patch Validated

QA Feedbacks
	*

