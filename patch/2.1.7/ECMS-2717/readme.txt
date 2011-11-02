Summary
	* Status: Parameterise option does not work properly in Content Explorer setting
	* CCP Issue: CCP-1035, Product Jira Issue: ECMS-2717.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Parameterise option does not work properly in Content Explorer setting

Fix description

How is the problem fixed?
	* Get the correct home path by concating "Home Path" of "Drive Name" and the selected path of "Select Path"

Patch information:
	* Patch files: ECMS-2717.patch

Tests to perform

Reproduction test
	1. Create a new page containing a site explorer.
	2. In the site explorer options, choose "Parameterise" and a drive.
	3. Select a path
	4. Save
	5. Open the custom site explorer -> File explorer doesn't point to the selected path ==>KO. In this page we aspect that opening the page we have to be jailed on the path that we set

Tests performed at DevLevel
	1. Create a new page containing a site explorer.
	2. In the site explorer options, choose "Parameterise" and a drive.
	3. Select a path
	4. Save
	5. Open the custom site explorer
	File explorer point to the selected path ==> OK


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
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
