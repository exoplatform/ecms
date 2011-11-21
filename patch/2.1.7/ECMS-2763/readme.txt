Summary
	* Status: Shared css and js is not reloaded after server restart
	* CCP Issue: CCP-1100, Product Jira Issue: ECMS-2763.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Shared css and js is not reloaded after server restart

Fix description

How is the problem fixed?
	* When loading style sheet and javascript via XSkin service, perform loading the individual portal site's style sheet and javascript before loading the shared css and js.

Patch files: ECMS-2763.patch

Tests to perform

Reproduction test

To reproduce w/ css file
	* Uploading to /Sites Management/shared/css in Content Explorer a CSS file
        	#ACMESignin {background-color:white;}
    	* Make sure that it is taken effect on web browser (background of Login portlet is white).
    	* Stop the server and restart it=>the background is now grey, the global css is not taken effect.
	* For reproduce w/ js file, upload or add a new js file in /Sites Management/shared/js and do the same steps w/ css file

Tests performed at DevLevel
	* Uploading to /Sites Management/shared/css in Content Explorer a CSS file
        	#ACMESignin {background-color:white;}
    	* Make sure that it is taken effect on web browser (background of Login portlet is white).
    	* Stop the server and restart it=>the background is now still white, the global css is taken effect --> OK

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

