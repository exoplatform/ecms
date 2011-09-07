Summary

	* Status: New group drive doesn't appear after group updating
	* CCP Issue: CCP-1032, Product Jira Issue: ECMS-2697.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* New group drive doesn't appear after group updating

Fix description

How is the problem fixed?
	* Add a listener to listen membership updating. This listener will be used to clear the cache of group which belong to current user

Patch information:
	Patch files: ECMS-2697.patch

Tests to perform

Reproduction test

Case 1:
On ECMS standalone
	# Login as john
	# Go to CE (you see 4 group drives)
	# Go in manage groups
	# Add john to /customers group (new membership)
	# Go in the CE => you still see 4 group drives, there should be 5 now

Case 2:
On PLF
	# Login as john and create a space validation1 whose registration is validation
	# Login as mary and request to access validation1
	# Login as john and approve this request
	# Login as mary -> we can access Document of validation1 (normal behaviour)
	# Do step1 -> step 4 in order to create another space validation2 whose registration is validation but we cannot access to Document of validation2 (abnormal behaviour). No /spaces/validation2 on Content Explorer but we can see right permission of mary on /spaces/validation2 in OrganizationService.

Tests performed at DevLevel
	* Do the same as 2 cases above and see that, user can access to the right space's Documents folder.

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes:
	* Add a listener into core-services-configuration.xml

Will previous configuration continue to work?
	* Yes.

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* No

Is there a performance risk/cost?
	* No.

Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	*

QA Feedbacks
	*
