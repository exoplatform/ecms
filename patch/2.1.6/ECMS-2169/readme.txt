Summary

	* Status: Cannot add value to a multi value boolean property
	* CCP Issue: CCP-898, Product Jira Issue: ECMS-2169.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Cannot add value to a multi value boolean property

Fix description

How is the problem fixed?
	* Disable the multi-value for Boolean type because the multi-value for Boolean type is non-sense in most of cases.

Patch information:
	Patch file: ECMS-2169.patch 

Tests to perform

Reproduction test

        - Go to Content Explorer
        - Create new node (content folder for example)
        - In System View choose 'View Node Properties'
        - Click Add New Property
        - Add "multiValueProperty" choose the Boolean type for this property and check it
	- In 'View Node Properties', click 'Edit' icon in 'Action' column of "multiValueProperty". You will see both checkboxes are unchecked
	- If you try to add a new value to the property, warning dialog "The field "value2" is required." will pop

Tests performed at DevLevel

	- Go to Content Explorer
	- Create new node (content folder for example)
	- In System View choose 'View Node Properties'
	- Click Add New Property
	- Add "multiValueProperty" choose the Boolean type for this property and check it
	- The multiple option is set to false and user can not edit the multiple option value

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
	* Patch Validated

Support Comment
	* Patch Validated

QA Feedbacks
	*
