Summary

    * Status: Illustration image is not saved when saving a Free Layout web content
    * CCP Issue: CCP-789, Product Jira Issue: ECMS-1995.
    * Complexity: N/A

The Proposal
Problem description

1)Add a new Free Layout web content "wc1" and fill mandatory fields
2)In Illustration tab, upload an illustration image
3)Save as draft and close

Expected Result :the uploaded illustration image is located under wc1/medias/images/illustration.
Actual Result :Under wc1/medias/images/illustration there is an empty image!

Observation :
If you edit that Free Layout web content and re-upload an illustration image,the image will be saved when clicking save as draft.
Fix description

How is the problem fixed?

    * The data of the uploaded file is not saved when node is created.
    * The binary file should be saved in bytes when putting to map in preparemap() in DialogFormUtil class

Patch files: ECMS-1995.patch

Tests to perform

Reproduction test

    * Add a new Free Layout web content "wc1" and fill mandatory fields
    * In Illustration tab, upload an illustration image
    * Save as draft and close
    * Under wc1/medias/images/illustration there is an empty image! => NOK

Tests performed at DevLevel

    * Do the same tasks like reproduction test => the uploaded illustration image is located under wc1/medias/images/illustration => OK

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

    * Function or ClassName change

Is there a performance risk/cost?
*No
Validation (PM/Support/QA)

PM Comment
* PL review: Patch validated

Support Comment
* Support review: Patch validated

QA Feedbacks
*
Labels parameters

