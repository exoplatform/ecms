Summary
    	* Status: IE7: Show confirm message when create new document with some templates
    	* CCP Issue: N/A, Product Jira Issue: ECMS-2374.
    	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* IE7: Show confirm message when create new document with some templates

Fix description

How is the problem fixed?
	* Add javascript:void(0) to action button to prevent the check for change in javascript code.

Patch files: ECMS-2374.patch

Reproduction test
	* Go to Content Explorer
    	* Select any drive
    	* Click on Add document icon in action bar
    	* Select template: Free layout web content or Site Search box webcontent or Site Navigation or Site Breadcumbs or Picture on head layout
    	* Fill values
    	* Save => Show message alert "the change will be lost if you navigate away from this page".

Tests performed at DevLevel
	* Go to Content Explorer
	* Select any drive
    	* Click on Add document icon in action bar
    	* Select template: Free layout web content or Site Search box webcontent or Site Navigation or Site Breadcumbs or Picture on head layout
    	* Fill values
   	* Save --> the document is saved and no warning alert message

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

