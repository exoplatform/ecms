Summary
    	* Status: Show duplicate message when click search in Edit mode
    	* CCP Issue: N/A, Product Jira Issue: ECMS-2423.
    	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Show duplicate message when click search in Edit mode

Fix description

How is the problem fixed?
    	* When search action is triggered, web browser sends 2 requests to server. One causes by onclick event on the div tag, one causes by the href of a tag. We should use only one, so remove the onclick event on the div tag
    	* Update javascript to call search event when hit Enter.

Patch files: ECMS-2423.patch

Tests to perform

Reproduction test
	* Click Edit mode icon in admin toolbar at right corner of screen
    	* Choose search portlet on navigation bar
    	* Check Page and Document
    	* Click Search=> Show duplicate message.

Tests performed at DevLevel
	* Do the same steps as Reproduction test => only one message appears.

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	*No

Configuration changes

Configuration changes:
	*No

Will previous configuration continue to work?
	*Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: N/A

Is there a performance risk/cost?
	*No

Validation (PM/Support/QA)

PM Comment
    	* Validated

Support Comment
    	* Validated

QA Feedbacks
	*

