Summary
    	* Status: Cannot edit field Label and Name of a free web content if we manipulated another free web content before
    	* CCP Issue: N/A, Product Jira Issue: ECMS-2379.
    	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
    	* Cannot edit field Label and Name of a free web content if we manipulated another free web content before

Fix description

How is the problem fixed?
    	* Focus to the first element in the form after loading successfully

Patch information:
	Patch files: ECMS-2379.patch

Tests to perform

Reproduction test
    	* Login
 	* Go to Content Explorer > Sites Management
	* Create a new document with free web content template, manipulate a little in this and save or close this document.
	* Create another new document with free web content template > Cannot edit Name and Title fields before refreshing web browser or selecting the Main content field.
	* This bug occurs only with web content templates. In using Firebug to detect this bug, there is an error on loading these templates.

Tests performed at DevLevel
	* Do the same steps as reproduction test => Can edit name & title field of free layout web content

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
    	* Function or ClassName change: N/A

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated on behalf of PM

Support Comment
    	* Validated

QA Feedbacks
	*

