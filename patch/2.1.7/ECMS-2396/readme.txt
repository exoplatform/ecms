Summary
    	* Status: Error on rendering pdf file whose name contains number in first character and dot character
    	* CCP Issue: N/A, Product Jira Issue: ECMS-2396.
    	* Complexity: Easy

The Proposal

Problem description

What is the problem to fix?
	* Error on rendering pdf file whose name contains number in first character and dot character

Fix description

How is the problem fixed?
	* Fix the way to get file name: replace name.substring(0, name.indexOf(.) by name.substring(0, name.lastIndexOf(.)

Patch files: ECMS-2396.patch

Tests to perform

Reproduction test
    	* Go to Site explorer/Site Management/acme/document
    	* Upload a pdf file with name includes number in first character and separate with characters by dot (e.g: 12.test.pdf) -> upload is successful.
    	* View uploaded file on Web Browser -> show code error in UI and throw exception in console

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
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

