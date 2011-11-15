Summary
	* Status: Form Builder: Nothing happen when click on Add item icon corresponding of fifth item in Select box
	* CCP Issue: N/A, Product Jira Issue: ECMS-2408.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Cannot add many options for a select box in Form Builder

Fix description

How is the problem fixed?
	* Change the attribute "Index" to "idx" because of "Index" is the reserved keyword for IE

Patch files: ECMS-2408.patch

Tests to perform

Reproduction test
	* Login and go Form builder porlet
	* Input Name and move to Form Buider tab
        	* Select Select box
        	* Then, edit and add about 5 default value: AAA,BBB,CCC,DDD,EEE -> add successful
        	* Next, Click onAdd item icon corresponding of fifth item (EEE) -> nothing happens and cannot add item for select box -> error

Tests performed at DevLevel
	*

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* N/A

Configuration changes

Configuration changes:
	* N/A

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

* Function or ClassName change

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

