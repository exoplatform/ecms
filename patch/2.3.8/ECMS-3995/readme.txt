Summary

    * Issue: Red layer display in front of preview image on activity stream
    * CCP Issue:  N/A
    * Product Jira Issue: ECMS-3954.
    * Complexity: N/A

Proposal

 
Problem description

What is the problem to fix?

    * When we upload a png with transparent layer, the transparent layer becomes red in preview mode.

Fix description

Problem analysis
    * The preview image is programmed as a JPEG image. So if the original image is a PNG image with transparency level (Alpha channel), it gives a wrong layer. 

How is the problem fixed?
    * Convert BufferImage to InputStream directly via ImageIO instead of JPEGImage to have a png image.

Tests to perform

Reproduction test

    * Go to Activity Stream. Upload a png image with transparent layer.
    * Problem: The transparent layer becomes red.

Tests performed at DevLevel

    * Cf. above

Tests performed at Support Level

    * Cf. above

Tests performed at QA

    * ...

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
    * No

Will previous configuration continue to work?
    * No

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * Function or ClassName change: 
    * Data (template, node type) migration/upgrade: 

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment

    * Patch validated.

Support Comment

    * Patch validated.

QA Feedbacks

    * ...
