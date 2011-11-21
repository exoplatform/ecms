Summary
	* Status: File 's status is not shown in case using WebDav
	* CCP Issue: N/A, Product Jira Issue: ECMS-2743.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* File 's status is not shown in uploading by WebDav

Fix description

How is the problem fixed?
	* Trigger when uploading a document, status is added if this file must have a document's status. No need add file status if we upload js, css, template and action file.

Patch files: ECMS-2743.patch

There are currently no attachments on this page.

Tests to perform

Reproduction test
	* Access link through webDAV e.g: rest/jcr/repository/collaboration
	* Login by james and goto /rest/jcr/repository/collaboration/sites content/live/acme/categories/acme
	* Drag and drop a file to acme >> succesfully
	* Login by john/root and go to http://localhost:8080/portal/rest/jcr/repository/collaboration/sites content/live/acme/categories/acme to check the drag and drop file above
        	* Actual result: The file is displayed without Status.
        	* Expected result: Draft status should be shown

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
	* Function or ClassName change : No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

