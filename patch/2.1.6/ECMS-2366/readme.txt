Summary
	
	* Status: Initialization error in DriverConnector service's constructor
	* CCP Issue: CCP-991, Product Jira Issue: ECMS-2366.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Constructor of this service calls resourceBundleService.getResourceBundle method, but if the ResourceBundleService has not started yet (start method of it has not been called), ResourceBundleService will cache wrong bundle.
	* Ensure getResourceBundle method is called after the "start" method of ResourceBundleService

Fix description

How is the problem fixed?
	* Change the way to get service at the first time those service is really required, not inside constructor.

Patch information:
	Patch files: ECMS-2366.patch

Tests to perform

Reproduction test
	1. On EPP-SP
		a. Run jboss
		b. Login in private mode: http://localhost:8080/ecmdemo/private/acme/ => Login page is empty and show exception in console.
	2. On ECMS standalone
		a. Run on developing mode
		b. Change platform's language on web browser into English
		c. Login in private mode: http://localhost:8080/ecmdemo/private/acme/ => Login page is empty and show exception in console.

Tests performed at DevLevel
	*

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* N/A

Configuration change

Configuration changes:
	* N/A

Will previous configuration continue to work?
	* N/A

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: No

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* PATCH VALIDATED

Support Comment
	* Patch validated

QA Feedbacks
	*
