Summary

	* Status: Translation content with country variant
	* CCP Issue: CCP-1052, Product Jira Issue: ECMS-2596.
	* Complexity: N/A

The Proposal

Problem description

What is the problem to fix?
	* Translation content for language without country variant.

Fix description

How is the problem fixed?
	* Add new case to get the translation node for LanguageService.
	* Add new case for CLV with folder mode to get the full language parameter (Language include Country Language and Country variant)

Patch information:
	 * Patch file: ECMS-2596.patch

Tests to perform

Reproduction test

Case 1:
	1. Modify a Article dialog template and add "zh" for language list.
	2. Create 2 contents of Article type, 1 for English and 1 for Simplified Chinese
	3. Select English content and Add translation of zh content.
	4. Publish all content types through Manage Publications.
	5. Add the content in a public page.
	6. Change the language to Simplified Chinese : The content is displayed in English -> Error
Case 2:
	1. Add 3 contents in English, 'zh', 'zh_CN' and publish them
	2. Add translation of 'zh' and 'zh_CN' for English content
	3. Add new category for English content (e.g acme/world)
	4. Create a new page and add CLV to this page
	5. Edit CLV and Select folder mode, add the category to CLV
	6. Open the new page and change language to Simplified Chinese, the 'zh_CN' content isn't displayed: not OK

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
	* Function or ClassName change : No

Is there a performance risk/cost?
	* N/A

Validation (PM/Support/QA)

PM Comment
	* Patch Validated

Support Comment
	* Patch Validated

QA Feedbacks
	*
