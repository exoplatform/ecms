Summary
    	* Status: Can't edit and remove comment when the folder name and file name contains apostrophe
    	* CCP Issue: N/A, Product Jira Issue: ECMS-2403.
    	* Complexity: Normal

The Proposal

Problem description

What is the problem to fix?
	* Can't edit and remove comment when the folder name and file name contains apostrophe

Fix description

How is the problem fixed?
	* Encode parameter "node path" in view1.gtmpl of comment component, also escape illegal characters of parameter "node path" in Comment and RemoveComment actions

Patch files: ECMS-2403.patch

Tests to perform

Reproduction test
    	* Go to Content Explorer| Sites Management| acme
    	* Create a folder having apostrophe character in its name, e.g. "test'sfolder"
    	* Create a document in this folder, e.g. "article1"
    	* Add a comment on that document
        * Show comment. Click "Edit this comment" icon. Nothing happens.
        * Remove Comment. Click "Remove this comment" icon -> Dialog popup and error on console
    	* Same problem occurs when trying to edit and delete comments on a new uploaded file whose name contains apostrophe character.

Tests performed at DevLevel
	*cf Above

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

