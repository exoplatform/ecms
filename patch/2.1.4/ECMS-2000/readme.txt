Summary

    * Status: Pager in Content Selector is not working in IE or Chrome
    * CCP Issue: CCP-794, Product Jira Issue: ECMS-2000.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
The paginator in the Content Selector is not working in IE or Chrome

Step 1 : Go to Toolbar mouse over 'Group' and click Content Explorer
Step 2 : Click in Content Explorer toolbar
Step 3 : Select template 'Free Layout Content' and next click WCM Content Selector icon system just open new window pop up
Step 4 : Select Medias filter in combo box
Step 5 : Go to folder containing many pictures (more than one page)
In Chrome: All nodes are shown in the same page:there is no paginator.
In IE8: The paginator is not visible (can't select the next page)
Fix description

How is the problem fixed?

    * change the number of rows per page by 12 instead of 14 for FF and 13 for other browser in the ContentSelector.js
    * move some code used to create paginator outside the for(..) loop:
    * modify code to show paginator in Chrome

Patch information:
Patch files: ECMS-2000.patch

Tests to perform

Reproduction test
Step 1 : Go to Toolbar mouse over 'Group' and click Content Explorer
Step 2 : Click in Content Explorer toolbar
Step 3 : Select template 'Free Layout Content' and next click WCM Content Selector icon system just open new window pop up
Step 4 : Select Medias filter in combo box
Step 5 : Go to folder containing many pictures (more than one page)
In Chrome: All nodes are shown in the same page:there is no paginator.
In IE8: The paginator is not visible (can't select the next page)

Tests performed at DevLevel
* Do the same tasks like Reproduction test => the paginator is showed correctly (12 rows per page) in FF, IE and Chrome

Tests performed at QA/Support Level
*

Documentation changes

Documentation changes:
*No

Configuration changes

Configuration changes:
*No

Will previous configuration continue to work?
*Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * No

Is there a performance risk/cost?
*No

Validation (PM/Support/QA)

PM Comment
* Validated by PM

Support Comment
* Support review: Patch validated

QA Feedbacks
*

