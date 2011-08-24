Summary

	* Status: Problems when manipulate file with path containing apostrophe character in CLV
	* CCP Issue: N/A, Product Jira Issue: ECMS-2410.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Problems when manipulate file with path containing apostrophe character in CLV

Fix description

How is the problem fixed?
	* Encode the url when calling action and decode the url when display on html page to escape the special characters

Patch information:
	Patch file: ECMS-2410.patch

Tests to perform

Reproduction test
	1. Login
	2. Go to *Content Explorer* > *Sites Management* > *acme* > *documents*
	3. Upload a document and enter a name with apostrophe character for it
	4. Add a new page > Add a Content List portlet (CLV)
	5. Switch *selection mode* to *by Contents*
	6. In multiple content selector panel, filename displays incorrectly and cannot save this choice.
	7. Same problem when creating a folder name containing apostrophe then create a new file on it then do the same cases.


Tests performed at DevLevel
	1. Login
	2. Go to *Content Explorer* > *Sites Management* > *acme* > *documents*
	3. Upload a document and enter a name with apostrophe character for it
	4. Add a new page > Add a Content List portlet (CLV)
	5. Switch *selection mode* to *by Contents*
	6. In multiple content selector panel, filename displays correctly and can save this choice.
	7. Confirmed the result with the folder's name containing the apostrophe character as above

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
	* N/A

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	* Patch Validated

QA Feedbacks
	*
