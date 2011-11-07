Summary
    	* Status: Can not load icon in Simple View
   	* CCP Issue: CCP-1109, Product Jira Issue: ECMS-2455.
    	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Can not load icon in Simple View

Fix description

How is the problem fixed?
	* Only rendering thumbnail icons after loading UISimpleView.js

Patch files: ECMS-2455.patch

Reproduction test
    	* Go to Group > Content Explorer
    	* Choose "/organization/management/executive-board"
    	* Click "Content Explorer" several time or refresh web browser.
    	* Icons can not be loaded and get this error in Firebug :
        	eXo.ecm.UISimpleView is undefined
        	eXo.ecm.UISimpleView.errorCallback(this);

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

