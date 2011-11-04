Summary
	* Status: Text float overflows the border and other part when using some templates in CLV
    	* CCP Issue: N/A, Product Jira Issue: ECMS-2407.
    	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Text float overflows the border and other part when using some templates in CLV

Fix description

How is the problem fixed?
	* Create correct Style sheet for the related groovy templates

Patch files: ECMS-2407.patch

Tests to perform

Reproduction test
    	* Login then edit or create new pages w/ CLV
    	* Choose UIContentListPresentationBigImage.gtmpl or UIContentListPresentationDefault.gtmpl or UIContentListPresentationSmall.gtmpl as template of this CLV > bug UI (Text float over the border)

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
    	* Function or ClassName change: No

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*

