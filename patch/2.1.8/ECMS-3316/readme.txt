Summary
	* Status: Impossible to apply two filters in the Filter By Type of File Explorer
	* CCP Issue: CCP-1196, Product Jira Issue: ECMS-3316.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Impossible to apply two filters in the Filter by type of File Explorer and the Content filter isn't executed.

Fix description

How is the problem fixed?
	* In case the "Contents" is not selected, we filter by mime type.
	* In case the "Contents" is selected, we filter by mime type then by content type. When we filter by content type, all contents (exclude folder) will be displayed.

Patch information:
	Patch files: ECMS-3316.patch

Tests to perform

Reproduction test
	* Access to Site Explorer and add some documents and images in a drive (EX Collaboration)
	* In the filter by type of the sidebar, choose two filters: Document and Image => Nothing is shown in the drive
	* The filter Contents is not working too

Tests performed at QA/Support Level
	* cf. above

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
	* N/A

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

