Summary

	* Status: WYSIWYG field not retaining its value on category selection in workflow
	* CCP Issue: CCP-1027, Product Jira Issue: ECMS-2501.
	* Fix also ECMS-2478
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix
	* If WYSIWYG field is edited in workflow, and after changing WYSIWYG field, category field is selected, WYSIWYG field does not retain the changed value. It populates the old saved value.

Fix description

How is the problem fixed?
	* Add needed javascript function to store data for CKEditor after each page-update
	* Check and do not initialize value for CKEditor if render process is not the first time render

Patch information:
	Patch file: ECMS-2501.patch

Tests to perform

Reproduction test

Steps to reproduce:
	1. Choose 'File' template
	2. Enter mandatory fields 'name' and 'content'
	3. Save document
	4. Start the workflow on this document (use publishing content workflow).
	5. Click on 'Manage' in workflow
	6. Go to Edit document tab
	7. Change the WYSIWYG field value
	8. Select a new category
	9. WYSIWYG field is reseted to old value.

Tests performed at DevLevel:
	* cf Above

Tests performed at QA/Support Level:
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
