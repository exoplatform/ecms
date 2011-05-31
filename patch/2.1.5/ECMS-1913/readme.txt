Summary

    * Status: Add Brazilian Portuguese translation :content is not shown
    * CCP Issue: CCP-743, Product Jira Issue: ECMS-1913.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?

How to reproduce :

1. Modifie dialog template and add Brazilian Portuguese (pt_BR) for language liste.
?
String[] fieldLang = ["jcrPath=/node/exo:language", "options=en,fr,es,,de,pt_BR", lang] ;

2. Create 3 content of type article, 1 each for english, french and Brazilian Portuguese.
3. In english content add translations of french and pt_br content.
4. Publish all 3 content types through Manage Publications.
5. Add the content in a public page.
6. Change the poratl lanquage to french : The french content is Display OK
7. Change the poratl lanquage to Brazilian Portuguese : The english content is Display KO

The code of Brazilian Portuguese is pt_BR

In our locales-config.xml, we have pt_BR for the Brazilian Portuguese.
?
<locale-config>
<locale>pt_BR</locale>
<output-encoding>UTF-8</output-encoding>
<input-encoding>UTF-8</input-encoding>
<description>Default configuration for the Brazilian Portugese locale</description>
</locale-config>

When you add translations of Brazilian Portuguese (pt_BR)
The Symlink of Brazilian Portuguese is add in this path :
?
http://localhost:8080/ecmdemo/rest-ecmdemo/jcr/repository/collaboration/Documents/Live/en-doc/languages/pt_BR/

Or when you try to view Brazilian Portuguese in the public page
?
getViewableContent(Node node, HashMap<String, String> filters)

try to get content by this path
?
http://localhost:8080/ecmdemo/rest-ecmdemo/jcr/repository/collaboration/Documents/Live/en-doc/languages/pt/

If you set pt code for the Brazilian Portuguese,You are getting correct translation for Brazilian Portuguese content.
Fix description

How is the problem fixed?

    * When get content to show, change the language filter to use [language]_[country] format instead of using [language]

Patch files:ECMS-1913.patch

Tests to perform

Reproduction test

1. Modify dialog template and add Brazilian Portuguese (pt_BR) into language list.String[] fieldLang = ["jcrPath=/node/exo:language", "options=en,fr,es,,de,pt_BR", lang] ;

2. Create 3 article contents, 1 each for english, french and Brazilian Portuguese.
3. In english content add translations of french and pt_br content.
4. Publish all 3 contents through Manage Publications.
5. Add the english content to a public page.
6. Change the portal lanquage to french : The french content is displayed => OK
7. Change the portal lanquage to Brazilian Portuguese : The english content is displayed => KO

Tests performed at DevLevel
* do the same tasks as reproduction test => translated content is displayed correctly

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
* PL review: patch validated

Support Comment
* Support review: patch validated

QA Feedbacks
*
Labels parameters

