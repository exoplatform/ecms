Summary
	* Status: Problem of displaying links in Content
	* CCP Issue: N/A, Product Jira Issue: ECMS-2607.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Problem of displaying links in Content

Fix description

How is the problem fixed?
	* Add style sheet for a:link, a:visited, a:hover and a:active when editing in CKEditor and view document in Site Explorer

Patch files: ECMS-2607.patch

Tests to perform

Reproduction test:
	1. Go to Content Explorer/Sites Management/acme/documents
	2. Add a document (e.g Free layout webcontent)
	3. Select Source mode
	4. Add a link:
		<a href="http://localhost:8080/ecmdemo">ecmdemo</a>
	5. Switch off Source mode, this link isn't displayed in right form
	6. Add links to the content by Insert Link button and Save as draft: these links aren't displayed in right form (DisplayLink.png)
		If we view the content in FO, links are displayed normally

Tests performed at DevLevel
 

Steps to reproduce:
	1. Go to Content Explorer/Sites Management/acme/documents
	2. Add a document (e.g Free layout webcontent)
	3. Select Source mode
	4. Add a link:
		<a href="http://localhost:8080/ecmdemo">ecmdemo</a>
	5. Switch off Source mode, this link is displayed in right form
	6. Add links to the content by Insert Link button and Save as draft: these links are displayed in right form

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

