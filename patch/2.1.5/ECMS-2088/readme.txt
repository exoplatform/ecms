Summary

    * Status: Some URL parameters are lost after returning from in-context editing in ContentExplorer portlet
    * CCP Issue: CCP-856 Product Jira Issue: ECMS-2088.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

After returning from in-context editing in ContentExplorer porlet all URL parameters are lost except the first one.
Steps to reproduce:
1) add Content Detail portlet to page and congigure it to display some content and view the page
2) request the page again with 2 or more URL parameters, for example: ?a=1&b=2&c=3
3) switch the portal to edit mode and perform Edit action on the content displayed in the ContentDetail portlet (ContentExplorer is opened in In-context-editing mode)
4) return back from ContentExplorer (green back-arrow)
All ampersands which separate the URL parameters are replaced by & which means all parameters are lost except the first one.
This causes problems when using both folder-id and content-id parameters for dynamic navigation in CLV/SCV portlets. It also affects other portlets on the page which depend on URL parameters.
Fix description

How is the problem fixed?

    *  Change the way to redirect web into ajaxRedirect

Patch files:ECMS-2088.patch

Tests to perform

Reproduction test
* cf. above

Tests performed at DevLevel
*

Tests performed at QA/Support Level
*
Documentation changes

Documentation changes:
* NO
Configuration changes

Configuration changes:
* NO

Will previous configuration continue to work?
* YES
Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change: None

Is there a performance risk/cost?
* No
Validation (PM/Support/QA)

PM Comment
* PL review: Patch validated

Support Comment
* Support review: Patch validated

QA Feedbacks
*

