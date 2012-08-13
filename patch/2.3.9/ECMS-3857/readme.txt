Summary

    Issue title Newsletter service not working well
    CCP Issue:  CCP-1338
    Product Jira Issue: ECMS-3857.
    Complexity: medium

Proposal

 
Problem description

What is the problem to fix?
* Cannot send newsletter template
* Sometimes we cannot delete or modify newsletter

Fix description

Problem analysis
We have two problems with this issue
* Cannot confirm for registering newsletter: Because the id for UINewsletterPortlet and its children are changed. So we cannot raise confirm action based on the id. 
* Delete and edit the Newsletter: The problem relates to click area of action popup

How is the problem fixed?
* Change the way redirect request in UINewsletterPortlet.
* Extend the click area when display action on the popup window.

Tests to perform

Reproduction test
* Cannot send newsletter template: after sending a newsletter and receive a mail, we cannot confirm this email
* Go to portlet newsletter, click at the border of Edit or Delete button of the newsletter.  Then action is not called

Tests performed at DevLevel
* cf. above

Tests performed at Support Level
* cf. above

Tests performed at QA
* cf. above

Changes in Test Referential

Changes in SNIFF/FUNC/REG tests
* No

Changes in Selenium scripts 
* No

Documentation changes

Documentation (User/Admin/Dev/Ref) changes:
* No

Configuration changes

Configuration changes:

    No

Will previous configuration continue to work?

    Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
* Function or ClassName change: No
* Data (template, node type) migration/upgrade: No

Is there a performance risk/cost?
* No

Validation (PM/Support/QA)

PM Comment
* Patch validated

Support Comment
* Patch validated

QA Feedbacks

