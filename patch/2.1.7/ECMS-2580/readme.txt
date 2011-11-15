Summary
	* Status: "Add/Manage Content" actions from CLV buggy when "Dynamic Navigation" mode active
	* CCP Issue: CCP-1045, Product Jira Issue: ECMS-2580.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* "Add/Manage Content" actions from CLV buggy when "Dynamic Navigation" mode active

Fix description

How is the problem fixed?
	* Fix the path to the current node.
	* Save the "back to" link in a private variable instead of preference.

Patch files: ECMS-2580.patch

Reproduction test

Case 1:
	* Login
	* Start a clean server
	* Go to $PORTAL/ecmdemo/private/acme/news then go to World category
	* Activate Edit mode
	* Add new content to category World in Dynamic Mode
	* Cannot find back button. Back button appears only we try to edit an available content in dynamic mode

Case 2:
	* Steps are the same with case 1
	* Error on place of new created document. It's always created in /acme

Case 3:
	* Continue from Case 1 when a back button appears.
	* Go to PORTAL/ecmdemo/private/acme/news then go to World category
	* Add new content to category World in Dynamic Mode
	* Fill new content then press Save as draft or Cancel
	* Press Back button -> the homepage is displayed, not World category

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

