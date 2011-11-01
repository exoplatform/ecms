ummary
	* Status: RSS feed links are rendered incorrectly
	* CCP Issue: N/A, Product Jira Issue: ECMS-2375.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* RSS feed links are rendered incorrectly

Fix description

How is the problem fixed?
	* Change the base link to generate the RSS

Patch information:
	* Patch files: ECMS-2375.patch

Tests to perform

Reproduction test
	1. Go to ACME > Overview in front office
	2. Click on the RSS link in "Latest News"
	3. On page of RSS links, click on a link -> Overview page is displayed instead of document content

Tests performed at DevLevel
	* C/f above

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* N/A

Configuration changes

Configuration changes:
	* N/A

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: No

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Validated on behalf of PM

Support Comment
	* Validated

QA Feedbacks
	*
