Summary
	* Status: Error UI if document list has more than one page
	* CCP Issue: N/A, Product Jira Issue: ECMS-2397.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Error UI if document list has more than one page

Fix description

How is the problem fixed?
	* Place the div of page iterator inside the div of content list
	* Change/Add the style="clear:both" right after the content

Patch files: ECMS-2397.patch

Tests to perform

Reproduction test
	* Login
	* Go to Sites Explorer/Sites management/acme/documents
	* Add some documents and publish them until document list has more than one page (in default, document list displays 5 documents per page.)
	* Go Overview page and see Document list -> error UI in page list

Tests performed at DevLevel
	* c/f above

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
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

