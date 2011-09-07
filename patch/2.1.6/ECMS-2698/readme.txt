Summary

	* Status: java.lang.ArrayIndexOutOfBoundsException is thrown when using method split of class NodeLocation
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2698.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* java.lang.ArrayIndexOutOfBoundsException appears when using method split of class NodeLocation

Fix description

How is the problem fixed?
	* Edit code to loop only three times to get three string items (repository, workspace, nodepath)

Patch information:

	Patch files: ECMS-2698.patch

Tests to perform

Reproduction test
	# Login as john
	# Create new page then add a Category Navigation portlet
	# In Display Settings, choose TagsCloud.gtmpl in Template
	# Save -> Exception and cannot render this page


Tests performed at DevLevel
	* Do the same steps as reproduction => have no Exception.


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
	* Function or ClassName change: No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	*

QA Feedbacks
	*
