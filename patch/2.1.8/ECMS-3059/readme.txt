Summary
	* Status: Image gets overwritten without warning
	* CCP Issue: CCP-1152, Product Jira Issue: ECMS-3059.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Image gets overwritten without warning

Fix description

How is the problem fixed?
	* When a file which has the same mime type with file in preview node is uploaded, it becomes a child node of the current node.

Patch information:
	Patch files: ECMS-3059.patch

Tests to perform

Reproduction test
	* Upload an image X in CE
	* Open it in preview mode
	* Use Upload file in Action Bar to upload an another image Y whose mimetime is the same with one of old image -> The original image Y that was in the preview gets replaced by the new uploaded image X keeping the same title but without any warning.

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

Function or ClassName change:
	- core/webui-explorer/src/main/java/org/exoplatform/ecm/webui/component/explorer/upload/UIUploadForm.java

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
