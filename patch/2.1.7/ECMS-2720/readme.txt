Summary

	* Status: Show wrong URL when in put title of node with special characters in IE 7 and IE 8
	* CCP Issue: CCP-1098, Product Jira Issue: ECMS-2720.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Show wrong URL when input title of node with special characters in IE 7 and IE 8

Fix description

How is the problem fixed?
	* Encode the Node's path following the URI name's convention.

Patch information:
	* Patch file: ECMS-2720.patch

Tests to perform

Reproduction test

Steps to reproduce:
	1. Log in as root at [http://localhost:8080/ecmdemo] (Using IE8 or IE7)
	2. In Edit mode in acme/overview, use the icon on the list viewer portlet to Add Content
	3. Create articles with titles 'Rakı','végétarisme' (Note that the name will be set to rakı as a result of the LocalizationConnector sanitation)
	4. Go back to the home page and open the new articles.
	5. A wrong content is shown in the detail portlet.
	
Tests performed at DevLevel
	*

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
	* Function or ClassName change : No

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
