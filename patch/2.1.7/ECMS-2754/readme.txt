Summary
	* Status: Can not select sub-node while inserting link to a site page into a web content
	* CCP Issue: N/A, Product Jira Issue: ECMS-2754.
	* Complexity: Normal

The Proposal

Problem description

What is the problem to fix?
	* Can not select sub-node while inserting link to a site page into a web content

Fix description

How is the problem fixed?
	* Add the missing part in node in order to display it correctly.

Patch files: ECMS-2754.patch

Tests to perform

Reproduction test
	* Login by admin
	* Go to Site Editor/Edit navigation
	* Add node at root path
	* Input Node name & select any page > Save
	* Right click on new added node & add new sub node with a specific page
	* Go to Site Editor/Add content
	* In the FCK editor toolbar click on the Insert link to a site page
	* Click Get Portal Link
	* Go to path of new added node at step 1 & 2 --> Can't select listed sub-node in right panel

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

