Summary
	*  Status: Click on "View document" in a space, the browser is redirected the space's home page.
	* CCP Issue: N/A, Product Jira Issue: ECMS-3302.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Click on "View document" in a space, the browser is redirected the space's home page.

Fix description

How is the problem fixed?
	*  We only need to remove the "/" character at the beginning of string "nodePathUrl", so the old way to get "portletId" will be wrong when the string "nodePathUrl" have many "/" characters (example: "/spaceName/Documents").
	* To fix this, we have to use the command nodePathUrl.substring(nodePathUrl.indexOf("/") + 1) to filter the "/" character at the beginning of string "nodePathUrl", instead of using the split 

Patch information:
	Patch files: ECMS-3302.patch
 
Tests to perform

Reproduction test
	* Go to documents page in a space
	* Upload a document
	* Right click it and choose "View document"
	* You'll be redirected to the space's home page instead of viewing the document

Tests performed at DevLevel
	* c.f above

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
	* Function or ClassName change: N/A

Is there a performance risk/cost?	
	* No
	
Validation (PM/Support/QA)

PM Comment
	* Validated
 
Support Comment
	* Validated

QA Feedbacks
	*

