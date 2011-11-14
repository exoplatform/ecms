Summary
	* Status: Empty error message while cancelling the creation of a document
	* CCP Issue: N/A, Product Jira Issue: ECMS-2378.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Empty error message when cancelling the creation of a document

Fix description

How is the problem fixed?
	* Store the i18n error messages in this case in a hidden html control then get these error messages from javascript by gettting them directly from html control
	* Fix also the regression of ECMS-2374 by adding the onchange event handle not only for IE but for all browser. This onchange will over-write the onchange event on the title field of some document template to call the eXo.ecm.SELocalization.cleanName() javascript function, so we change these events to onblur.

Patch files: ECMS-2378.patch

Reproduction test

Only reproduced on Firefox and Chrome
	* Login
	* Go to Content Explorer > Sites Management > acme > documents
	* Create new article
	* Fill several fields with some characters
	* Click Close to cancel this creation -> A Javascript alert is poping up with an EMPTY Message

Tests performed at DevLevel
	* Login
	* Go to Content Explorer > Sites Management > acme > documents
	* Create new article
	* Fill one or/and several fields with some characters
	* Click Close to cancel this creation -> A Javascript alert is poping up with a i18n relevant confirm message

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

