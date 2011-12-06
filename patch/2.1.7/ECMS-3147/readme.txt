Summary
	* Status: Problem on editing a CLV
	* CCP Issue: N/A, Product Jira Issue: ECMS-3147.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Problem on editing a CLV

Fix description
How is the problem fixed?
	* This issue contains the following bugs:
		* Javascript bug: javascript warning appears when we edit CLV portlet in second time.
		* UI bug: The UICLVConfig form doesn't appear when we edit CLV porlet right after saving it.
		* Template File does't allow script code in content field. It affects the modification of any existed template or the creation of any template.
	*How to fix it?
		* Javascript bug: Fix the grammatical error on UIContentListPresentationDefault.gtmpl
		* UI Bug: This error appears if we notify user a message when successfully store data of CLV config form before. So to fix it, we won't notify the message when storing is completed.
		* Add option "noSanitization" for content field in dialog template of file content type.

Patch files: ECMS-3147.patch

Tests to perform

Reproduction test
	* Problem with CLV
		* Create a page then add a CLV into it.
		* Choose a path or some documents displayed on CLV.
		* Re-edit this CLV -> no Edit form is displayed
		* Save this page.
		* Go to Page Editor -> Edit page to edit new added CLV -> Error on javascript
	* Problem on editing a template
		* Edit any available view on any template -> After saving, all double quotes are encoded -> cannot use this template anymore

Tests performed at DevLevel
	* cf above

Tests performed at QA/Support Level
	* cf above

Documentation changes

Documentation changes:
	*  No

Configuration changes

Configuration changes:
	*  No

Will previous configuration continue to work?
	*  Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* N/A

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	*  Validated on behalf of PM

Support Comment
	* Validated

QA Feedbacks
	*

