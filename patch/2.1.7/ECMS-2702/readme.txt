Summary
	* Status: SearchAllVersions property of the query function of the Discrovery Service is ignored
	* CCP Issue: N/A, Product Jira Issue: ECMS-2702.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* SearchAllVersions property of the query function of the Discrovery Service is ignored

Fix description

How is the problem fixed?
	* StorageImpl.query(Query query)
	* Added the part of code for update the realQuery with the appropriate value for the "cmis:isLatestVersion" operand.

Patch information:

Patch files: ECMS-2702.patch

Tests to perform

Reproduction test
	* It is now possible to search on all version of document.
	* However it's not possible to search only on the latest version anymore.

Tests performed at DevLevel
	* JUnits and a new test testQueryWithSearchAllVersionFalse

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
	*

Support Comment
	*

QA Feedbacks
	*

