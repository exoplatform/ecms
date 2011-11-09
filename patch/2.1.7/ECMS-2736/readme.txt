Summary
	* Status: XSS attack on creating new document
	* CCP Issue: N/A, Product Jira Issue: ECMS-2736.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* XSS attack on creating new document

Fix description

How is the problem fixed?
	* Execution javascript is allowed on the field which have the option noSanitization in template. By default - javascript content will be removed

Patch files: ECMS-2736.patch

Reproduction test
	* Login and go to CE
	* Add a new document in using free web content template
	* In main content field, change to Source mode and paste the following instruction
 		<a onclick="return alert('XSS here')">XSS attack</a>
	* Save this article and click on line XSS attack, a pop-up menu appears

Tests performed at DevLevel
	* cf. above

Tests performed at QA/Support Level
	* cf. above

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
	* N/A

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

