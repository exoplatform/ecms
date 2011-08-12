Summary

	* Status: Bugs when search and download documents whose name contains apostrophe character
	* CCP Issue: CCP-978, Product Jira Issue: ECMS-2317.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Bugs when search and download documents whose name contains apostrophe character

Fix description

How is the problem fixed?
	* Unescape the illegal JCR characters and save it into the exo:title property of the document
	* Use SimpleSearchValidator to validate the Simple Search Text Input (validate empty value only)
	* Catch Exception and show message about unsupported characters

Patch information:
	Patch file: ECMS-2317.patch


Tests to perform

Reproduction test
	1. Login as root
	2. Go to Content Explorer > Sites Management > acme > documents
	3. Upload new document with apostrophe in name (for example "ap'str.doc")
	4. When we search in front office a document whose name contains apostrophe character, document's name appears "ap%27str.doc" and it cannot be downloaded.

Tests performed at DevLevel
	* Do the same steps as reproduction test => you can search the document whose name contains apostrophe and display name format is valid.


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
	* Function or ClassName change: No

Is there a performance risk/cost?
	* No


Validation (PM/Support/QA)

PM Comment
	* PATCH VALIDATED

Support Comment
	* Patch validated

QA Feedbacks
	*
