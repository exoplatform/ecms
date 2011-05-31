Summary

    * Status: Leak exception in method getLivePortalsStorage
    * CCP Issue: CCP-861, Product Jira Issue: ECMS-2103.
    * Complexity: N/A

The Proposal
Problem description

What is the problem to fix?
Some french translations are missing
View attached image in jira: ECMS-2103
Expected fix:
To be translated with :

Download and Allow Edition => Télécharger pour édition
Owned by me => Créés par moi
Favorites => Favoris
Hidden => Cachés
Content => Contenu
Document => Document
Image => Image
Music => Musique
Video => Vidéo
Also, to be coherent, fix in english :

Contents => Content
Documents => Document
Images => Image
Fix description

How is the problem fixed?

    * Translating the labels in French
    * Adding the new labels in French

Patch files: ECMS-2103.patch

Tests to perform

Reproduction test

    * Login ECMS
    * Change language to French
    * Go to Content Explorer, we can see some french translations are missing

Tests performed at DevLevel

    * cf above

Tests performed at QA/Support Level

    * cf above

Documentation changes

Documentation changes:

    * No

Configuration changes

Configuration changes:

    * No

Will previous configuration continue to work?

    * Yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * N/A.

Function or ClassName change

    * apps/portlet-explorer/src/main/webapp/WEB-INF/classes/locale/portlet/explorer/JCRExplorerPortlet_fr.xml
    * apps/portlet-explorer/src/main/webapp/WEB-INF/classes/locale/portlet/explorer/JCRExplorerPortlet_en.xml

Is there a performance risk/cost?

    * No

Validation (PM/Support/QA)

PM Comment

    * PL review: patch validated

Support Comment

    * Support review: patch validated

QA Feedbacks
*

