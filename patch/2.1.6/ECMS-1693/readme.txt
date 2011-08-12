Summary

	* Status: Unable to remove a permission on a node template
	* CCP Issue: N/A, Product Jira Issue: ECMS-1693.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Only able to add new permission, unable to remove one.

Fix description

How is the problem fixed?
	* Add remove permission button for removing all selected permissions

Patch information:
	Patch file: ECMS-1693.patch

Tests to perform

Reproduction test
	1. Login as john
	2. Go to Group > Administrator > Content Presentation > Manage Templates
	3. Click Edit icon to edit exo:article template
	4. Switch to Dialog tab of edit template form
	5. Click Edit icon to edit dialog1.gtmpl groovy template
	6. We can only add more permissions, cannot delete the selected permission

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
	* YES

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: N/A

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* PATCH VALIDATED

Support Comment
	* Patch validated

QA Feedbacks
	*
