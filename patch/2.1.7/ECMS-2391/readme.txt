Summary
	* Status: Saved search does not provide any result while showing list of drives in Site Explorer
	* CCP Issue: N/A, Product Jira Issue: ECMS-2391.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Saved search does not provide any result while showing drive list in Site Explorer

Fix description
How is the problem fixed?
	* When showing the drives list, the drive panel overlaps on the result page.
	* Remove the drive panel, then re-add the result page.

Patch information:
	* Patch files: ECMS-2391.patch

Tests to perform

Reproduction test
	1. Go to Site Explorer
	2. When the right container shows the list of drives, Click on *Saved searches*
	3. Click on any saved search -> Nothing changes, the result is not shown

Tests performed at DevLevel
	* c/f above

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* N/A

Configuration changes

Configuration changes:
	* N/A

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
