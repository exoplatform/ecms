Summary
	* Status: Incorrect mimetype assigned to images by "RESTImagesRendererService"
	* CCP Issue: CCP-931, Product Jira Issue: ECMS-2358.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Incorrect mimetype assigned to images by "RESTImagesRendererService"

Fix description

How is the problem fixed?
	* Get the "jcr:mimeType" property from the image as return MIME type first, if that property is not exist, set the using the default MIME type as "image/jpg" because this MIME type is supported by almost current browser.

Patch information:
	* Patch files: ECMS-2358.patch

Tests to perform

Reproduction test
	* RESTImagesRendererService assigns the mimetype "image" for images, which can cause unexpected behavior in the browser. It should give a true mimetype from "JCR: mimetype" property if the property exists.

Tests performed at DevLevel
	*

Tests performed at QA/Support Level
	*

Documentation changes

Documentation changes:	
	* N/A

Configuration changes

Configuration changes:
	* N/A

Will previous configuration continue to work?
	* Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?
	* Function or ClassName change: N/A

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Validated

Support Comment
	* Validated

QA Feedbacks
	*
