Summary

Status: Cannot load and merge the bundle
CCP Issue: CCP-761 , Product Jira Issue: PORTAL-3927.
Complexity: N/A

The Proposal

Problem description
What is the problem to fix?
On windows environment : set "JAVA_OPTS=%JAVA_OPTS% -Duser.language=zh -Duser.region=CN" in run.conf.bat
On Linux environment :you do "export LANG=zh_CN.UTF-8"
start the EPP 5.1 instance with Site Publisher.

==>the following errors occur:

ERROR [resources] Cannot load and merge the bundle: merge:locale.wcm.dialogs_locale.wcm.views_locale.wcm.webui_locale.portal.expression_locale.portal.services_locale.portal.webui_locale.ecm.webui_locale.portal.custom_locale.ecm.dialogs_locale.ecm.views_locale.ecm.homepage_locale.navigation.group.platform.administrators_locale.navigation.group.platform.users_locale.navigation.group.platform.guests_locale.navigation.group.organization.management.executive-board_locale.portlet.explorer.JCRExplorerPortlet_locale.portlet.administration.ECMAdminPortlet_locale.services.publication.lifecycle.authoring.AuthoringPublication_locale.services.publication.lifecycle.simple.SimplePublication_locale.services.publication.lifecycle.stageversion.StageAndVersionPublication_locale.navigation.portal.classic_locale.navigation.portal.acme_locale.navigation.group.platform.web-contributors_locale.portal.demo_webui_Resource_pt_BR
java.util.MissingResourceException: Can't find bundle for base name locale.portlet.explorer.JCRExplorerPortlet, locale pt_BR
at java.util.ResourceBundle.throwMissingResourceException(ResourceBundle.java:1427)
at java.util.ResourceBundle.getBundleImpl(ResourceBundle.java:1250)
at java.util.ResourceBundle.getBundle(ResourceBundle.java:952)
at org.exoplatform.services.resources.ResourceBundleLoader.load(ResourceBundleLoader.java:41)
I've tried to:
1) add locale support to the portlet.xml:
<supported-locale>zh</supported-locale>
<resource-bundle>locale.portlet.explorer.JCRExplorerPortlet</resource-bundle>
2) add JCRExplorerPortlet_zh.xml to ecmexplorer.war/WEB-INF/classes/locale/portlet/explorer
But the same issue still takes place.

Fix description
How is the problem fixed?

The problem comes from ResourceBundleConnector class in WCM. We just used the current locale only to get the resource bundle, but in some cases it needed to plus the country variant. So to fix this problem we plus the country variant value with the current locale.

Patch information:

Patch files:ECMS-1952.patch

Tests to perform
Reproduction test
*cf. above

Tests performed at DevLevel
* Do the same steps as above and make sure no exception or waring raising.

Tests performed at QA/Support Level
*

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

No side effect with this fix. Classes changed: packaging/wcm/webapp/src/main/webapp/groovy/portal/webui/workspace/UIWorkingWorkspace.gtmpl and core/connector/src/main/java/org/exoplatform/wcm/connector/collaboration/ResourceBundleConnector.java
Is there a performance risk/cost?
* No, there isn't.

Validation (PM/Support/QA)

PM Comment
* PL review: Patch validated

Support Comment
* Support review: Patch validated

QA Feedbacks
*

Étiquettes :

