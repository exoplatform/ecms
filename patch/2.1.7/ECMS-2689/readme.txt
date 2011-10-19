Summary
	* Status: Any new added action is not triggered any more after restart
	* CCP Issue: CCP-1083, Product Jira Issue: ECMS-2689.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Any new added action is not triggered any more after restart

Fix description

How is the problem fixed?
	* Add service which restores all triggered Actions on nodes when server is restarted 

Patch information:
	* ECMS-2689.patch

Tests to perform

Reproduction test
	1. Login then go to Content Explorer > Sites Management > acme > Documents then create any folder
	2. Click on the created folder and after that click on the *Manage Actions* tab > *Add Action* tab
		a. Select the *exo:autoVersioning* in the *Create an Action of Type* combo-box
		b. Choose any name to the action
		c. Select *Content Addition* in the *lifecycle*
		d. Save
	3. Create a new content in the folder created, could be an Article for instance and save it
	4. Open the content and click on the *Manage Versions* tab, see that it was created a new version. The *AutoVersioning Action worked fine*
	5. Restart ECMS
	6. Add a new content in the same folder created in the step 1
	7. Open the new content and click on the *Manage Versions*, you can see that the AutoVersioning Action wasn't invoked and there is no new version

Tests performed at DevLevel
	* C/f above

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes:
	* Add ActionMigrationService to core service

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: Add class ActionMigrationService.java

Is there a performance risk/cost?
	* Yes, when starting server, server takes time to restore triggered Actions on nodes

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
