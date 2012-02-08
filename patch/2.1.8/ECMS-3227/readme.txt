Summary
	* Status: Hidden Field's value is reseted while editing a document
	* CCP Issue: CCP-1177, Product Jira Issue: ECMS-3227.
    	* Complexity: Normal

The Proposal

Problem description

What is the problem to fix?
	* Hidden Field's value is reseted while editing a document

Fix description

How is the problem fixed?
	* We only check and set a property of a node from null to empty if this value is null.

Patch information:
	Patch files: ECMS-3227.patch

Tests to perform

Reproduction test
	* Create a new node type "exo:mynodetype" having "nt:base" as super-type and a String property called "exo:content"
	* Create new template named mynodetype in using the new node type and use the content of file dialog.gtmpl attached on JIRA as dialog
	* Create node with mynodetype as teamplate and set exo:content property "content of my node" then, make sure that the node holds the right value the property in property management
	* Go back to site administration, edit dialog of new template and make exo:content property hidden by commenting lines 7-8 and uncommenting lines 5-6.
	* Go back to site explorer, edit the node of step 3) and save.
	* Open Property management pop-up to view this node's properties -> This property is reset(it's empty)

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

