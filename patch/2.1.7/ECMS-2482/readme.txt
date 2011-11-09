Summary
	* Status: Show exception when view language in File Plan document
	* CCP Issue: N/A, Product Jira Issue: ECMS-2482.
	* Complexity: Normal

The Proposal

Problem description

What is the problem to fix?
	* Show exception when view language in File Plan document

Fix description

How is the problem fixed?
	* Check actionNode object in BaseActionLauncherListener. If it is null, return and do no process any more

Patch files: ECMS-2482.patch

Tests to perform

Reproduction test
	* Select this document
	* Select Collaboration tab
	* Click 'Add/Edit localized contents' icon
	* Input valid data in all fields
	* Click Save button
	* All fields are inputted data
	* File Plan document is added language
	* Select language which added above and click View -> error

Tests performed at DevLevel
	* cf Above

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
	* No

Function or ClassName change
	* No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

