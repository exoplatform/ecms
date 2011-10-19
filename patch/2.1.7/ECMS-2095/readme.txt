Summary
	* Status: Unknown error when do action on document while trying to edit
	* CCP Issue:* CCP-1102, Product Jira Issue: ECMS-2095.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Unknown error when do action on document while trying to edit

Fix description

How is the problem fixed?
	* Add lock token for node if it has been locked before changing its publication state

Patch information:
	* ECMS-2095.patch

Tests to perform

Reproduction test
	1. Login as root
	2. Go to Sites Explorer/Sites Management/acme/documents
	3. Add new document
	4. Add SCV page
	5. Browse to added document to be view SCV
	6. Switch to Edit mode
	7. CLick edit icon to edit the document (inside SCV)
	8. Click Request Approval or Publish -> Unknown error

Tests performed at DevLevel
	* Do the same as steps to reproduction => have no any exception's thrown

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
	* Function or ClassName change

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated on behalf of PM

Support Comment
	* Validated

QA Feedbacks
	*
