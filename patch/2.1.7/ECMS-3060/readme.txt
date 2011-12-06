Summary
	* Status: Popup message appears on adding new content
	* CCP Issue: N/A, Product Jira Issue: ECMS-3060.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Popup message appears on adding new content

Fix description

How is the problem fixed?
	* Move the javascript function named autoFocus() to WCMUtil.js to load at startup

Patch files: ECMS-3060.patch

Tests to perform

Reproduction test
	This problem occurs only on Firefox 3.6.x, not on Internet Explorer 7.0 or higher, Firefox 4.0 or higher and Chrome 12.x or higher
		* Go to Content Explorer, select 1 drive
		* Select 1 folder and click on Add Content icon in action bar --> pop-up message appears

Tests performed at DevLevel
	* Do the same step as Reproduction Test => pop-up's disappeared

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

