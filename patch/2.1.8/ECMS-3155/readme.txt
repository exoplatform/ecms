Summary
	* Status: Error in date time while approving a request
	* CCP Issue: N/A, Product Jira Issue: ECMS-3155.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Error in date time when approving a request. The server creates use only current month to name the folder for both start date and end date

Fix description

How is the problem fixed?
	* The name in live folder is the the start day containg start date for publication and 6 days after that Monday
	* We need to create folder to store content with correct naming rule after approving validation requesting content [exo:publishingProcess]. Before this fix, name of month of each start date and 1 week after start date are determined by the month name of 1 week after start date. We need to separate 2 these moments.

Patch information:
	Patch files: ECMS-3155.patch

Tests to perform

Reproduction test
	1. Log in as James, Create a request in validation folder	
	2. Log in as John, Go to Business Process Controller, click Manage, choose start and end time, click Approve
	3. Log in as James, go to CE/ collaboration drive/ document/ live
	-> Issue: error in date time of this request

Tests performed at DevLevel
	* c/f above

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
	* Function or ClassName change: ext/workflow/bp-jbpm-content/src/main/java/org/exoplatform/processes/publishing/ProcessUtil.java

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

