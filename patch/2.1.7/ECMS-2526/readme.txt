Summary
	* Status: Bug in "View Properties" Component
	* CCP Issue: CCP-1033, Product Jira Issue: ECMS-2526.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Bug in "View Properties" Component

Fix description

How is the problem fixed?
	* Using StringEscapeUtils.escapeHtml(String text) to encode the original text into HTML format before putting it onto GUI
	* For example:
		** Original text: "bread" & "butter" 
		** HTML text: &quot;bread&quot; &amp; &quot;butter&quot;

Patch information:
	* Patch files: ECMS-2526.patch

Tests to perform

Reproduction test
	1. Go to Site Explorer and select article template
	2. Create an article and paste the HTML content containing <table> tags into the source view of the WYSIWYG of summary field.
	3. Save the content
	4. Click on "View Node Properties" and you will find that the dialog of the component is not shown.
	5. If you refresh the browser window then you will see the dialog with broken UI.

Tests performed at DevLevel
	* Do the same step as reproduction => have no broken UI on "View Node Properties" dialog

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
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
