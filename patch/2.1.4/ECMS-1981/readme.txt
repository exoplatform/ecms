Summary

    * Status: Content by URL Portlet does not show right content for public users
    * CCP Issue: CCP-777, Product Jira Issue: ECMS-1981.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

    * Content by URL Portlet does not show right content for public users.

Fix description

How is the problem fixed?

    * Add the PorletFilterCache for the ParameterizedContentViewer portlet

Patch information:
Patch files: ECMS-1981.patch

Tests to perform

Reproduction test
*Steps to reproduce

1. Create 2 contents(content1, content2) in the repository and publish them.
2. Create a new Page in the Portal and drop Content by URL portlet. Configure the portlet to show content by query parameter in the URL
in Content Selection select "content1"
go to --> Advanced
-->Dynamic Navigation
in Contextual Content select Enabled and make the value by "path")
4-save the portlet setting
5-disconnect
6. Open the new portal page and point the portlet to content1 by passing the path of the content in the URL.
ex: http://localhost:8080/ecmdemo/page?path=/repository/collaboration/sites content/live/acme/content1
7. In second tab of the browser, open the page with path parameter pointing to the second content.
ex: http://localhost:8080/ecmdemo/page?path=/repository/collaboration/sites content/live/acme/content2
8. Reload tab1 and tab2 multiple times
9. You will see that data of content1 URL is shown in the page of content2 and vice versa.
The issue does not occur for logged in users.

Tests performed at DevLevel
*

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
* No

Configuration changes

Configuration changes:
* Cf. above

Will previous configuration continue to work?
* No.

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change: no

Is there a performance risk/cost?
* No

Validation (PM/Support/QA)

PM Comment
*Validated by PM

Support Comment
* Support review: Validated

QA Feedbacks
*

