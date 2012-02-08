Summary
	* Status: Issues on creating and/or deleting javascript, css files from Content Explorer
	* CCP Issue: CCP-961, Product Jira Issue: ECMS-2297.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Issues on creating and/or deleting javascript, css files from Content Explorer

Fix description

How is the problem fixed?
	* Sort the javascript data by priority, get all activated javascript (css) data and then merge into one by the priority.

Patch information:
	Patch files: ECMS-2297.patch

Tests to perform

Reproduction test
	1. Go to ecmdemo/private/acme/siteExplorer
	2. Select "Sites Management"
    	3. Go to folder /acme/js
    	4. Add JS document Test.js containing alert("here is Acme site");
	
Problem 1: Problem loading JS in the creation:
	5a. Go to Front-page site ACME the adding script is not loaded in merge.js "ecmdemo/javascript/eXo/ merged.js" (we have already cleared the cache)
        6a. To load adding script in merged.js by go to Sites Management/acme/js/test.js -> right click > Edit > save and Clear the cache browser.
Problem 2: Remove JS scipt:
        5b. If a file is deleted from js Content Explorer, It is Still Affecting The Portal page (browser cache cleared). We must restart the server or just delete the content of js document with cleared browser cache

The same problem is detected for css.

Tests performed at DevLevel
	* Do the same steps as reproduction test, so:

    		case #1: one alert is showed on ACME site
    		case #2: one alert is showed after deleting the JS file.

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

