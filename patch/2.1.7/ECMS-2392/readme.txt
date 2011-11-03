Summary
    	* Status: Cannot upload a zero file
    	* CCP Issue: N/A, Product Jira Issue: ECMS-2392.
    	* Complexity: Normal

The Proposal

Problem description

What is the problem to fix?
	* Cannot upload a zero file

Fix description

How is the problem fixed?
	* Check UploadData stream from UIFormUploadInput. If it is null, we replace it by an empty ByteArrayInputStream.

Patch information:
	Patch files: ECMS-2392

Reproduction test
	* Login by root
	* Go to ACME ->Groups ->Sites Explorer
	* Upload a file with size is "0" and save-> Unknown error and exception is raised in console

Tests performed at DevLevel
	* cf Above

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

Function or ClassName change: No

Is there a performance risk/cost?
    	* No

Validation (PM/Support/QA)

PM Comment
    	* Validated

Support Comment
    	* Validated

QA Feedbacks
	*

