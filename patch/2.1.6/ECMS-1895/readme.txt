Summary

	* Status: RTF documents edition dialog should provide an upload box instead of the text area
	* CCP Issue: CCP-734, Product Jira Issue: ECMS-1895.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* RTF documents edition dialog should provide an upload box instead of the text area

Fix description

How is the problem fixed?
	* Check for the MIME file type inside the file node type, in the case of "Text/RTF" display the upload process instead of the text box. Use external plugin to manage MIME file types 

Patch information:
	Patch file: ECMS-1895.patch

Tests to perform

Reproduction test
	* RTF documents edition dialog should be similar to .doc one. It should contain an upload box instead of the text area because RTF format is not human readable and it is not convenient to edit an RTF file without a dedicated editor.

Tests performed at DevLevel
	*

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

Function or ClassName change : No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* PATCH VALIDATED

Support Comment
	* Patch validated

QA Feedbacks
	*
