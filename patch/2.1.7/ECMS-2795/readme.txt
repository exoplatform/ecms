Summary
    	* Status: Random jquery-1.5.1.js script include
    	* CCP Issue: CCP-1122, Product Jira Issue: ECMS-2795.
    	* Complexity: Normal

The Proposal

Problem description

What is the problem to fix?
    * The script tag to import "jquery-1.5.1.js" lib sometimes appears, sometimes disappears in the HEAD tag of web page

Fix description

How is the problem fixed?
    * Because current version of flowplayer requires jquery to run, so we embedded jquery in our product. New flowplayer version does not need jquery any more. So the solution is to upgrade flowplayer to the newest version and remove jquery in ECMS

Patch files: ECMS-2795.patch


Tests to perform

Reproduction test
    	* Connect to acme site with public access ecmdemo/public/acme
    	* View the page code source
    	* Try multiple refreshes => The last item in the HEAD tag will sometimes be that jquery-1.5.1.js and sometimes it won't be there.

Tests performed at DevLevel
	* cf Above

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:
	* No

Configuration changes

Configuration changes
Add files:
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer-3.2.6.min.js
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer.audio-3.2.2.swf
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer.controls-tube-3.2.5.swf
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer-3.2.7.swf
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer.controls-3.2.5.swf
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer.pseudostreaming-3.2.7.swf

Remove unused files:
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer-3.1.4.min.js
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer.audio-3.1.2.swf
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer.controls-tube-3.1.5.swf
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer-3.1.5.swf
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer.controls-3.1.5.swf
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/jquery-1.3.2.js
	apps/portlet-explorer/src/main/webapp/javascript/eXo/ecm/flowplayer.playlist-3.0.7.js
	apps/resources-wcm/src/main/webapp/javascript/eXo/wcm/frontoffice/public/jquery-1.5.1.js

Will previous configuration continue to work?
	* Yes
Risks and impacts

Can this bug fix have any side effects on current client projects?	
	* No

Function or ClassName change
    	* No

Is there a performance risk/cost?
	* No

Validation (PM/Support/QA)

PM Comment
	* Validated on behalf of PM

Support Comment
	* Validated

QA Feedbacks
	*
