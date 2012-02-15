Summary
	* Status: Wrong icons for MS office documents in File Explorer
	* CCP Issue: CCP-1154, Product Jira Issue: ECMS-3079.
	* Complexity: N/A

The Proposal
	
Problem description

What is the problem to fix?
	* Wrong icons for MS office documents in File Explorer because PLF server cannot recognise correctly file type.

Fix description

How is the problem fixed?
	* Add the relevant css icons for below MS Office documents' mime type
		application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
		application/vnd.openxmlformats-officedocument.presentationml.presentation
		application/vnd.openxmlformats-officedocument.wordprocessingml.document

Patch information:
	Patch files: ECMS-3079.patch

Tests to perform

Reproduction test
	* Access to Sites eXplorer
	* Upload some documents (.docx, .xlsx, .pptx) => The associated icons for each document are wrong (see attachement)

Tests performed at DevLevel
	* Goto Sites Explorer
	* Upload some documents (.docx, xlsx, .pptx,...) => The associated icons for each document are right

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

