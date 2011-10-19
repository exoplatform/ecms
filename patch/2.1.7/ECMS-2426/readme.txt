Summary
	* Status: Images are not always available in ACME site
	* CCP Issue: CCP-1005, Product Jira Issue: ECMS-2426.
	* Complexity: Normal

The Proposal

Problem description

What is the problem to fix?
	* Images are not always available in ACME site

Fix description

How is the problem fixed?
	* Make sure that image property "jcr:lastModified" exists before getting it from jcr:content in versionStorage

Patch information:
	* ECMS-2426.patch

Tests to perform

Reproduction test
Case 1:
	1. Login then go to [http://localhost:8080/ecmdemo/private/acme]
	2. Change to *Edit* mode
	3. Edit one document which is found under acme logo > Save as draft > Republish it.
	4. Return [http://localhost:8080/ecmdemo/private/acme] then refresh this page, illustration of republished document is visible then disappears and reappears after refreshing pages

Case 2:
	1. Login then create a new document in using Free Web Content template in the same document. It contains an illustration.
	2. Create new page then add *Content List* Portlet. Choose directory containing this new web content.
	3. In this page, the illustration of document is visible then disappears and reappears  after refreshing pages

Tests performed at DevLevel
	* cf above

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
	* No

Function or ClassName change
	* No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated on behalf of PM

Support Comment
	* Validated

QA Feedbacks
	*
