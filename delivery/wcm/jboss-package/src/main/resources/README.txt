1. Installation of eXo Content
Drop EARs files in your JBoss application with already a compatible portal installed

2. Using navigation controller to display sitemaps.xml and robots.txt on SEO
* Open the controller.xml file (Nomally, this file is placed in /Jboss_home/server/default/config/gatein/ folder).
* Paste the following configuration into controller.xml:

  <route path="/{gtn:sitename}/sitemaps.xml">
    <route-param qname="gtn:handler">
      <value>sitemap</value>
    </route-param>
  </route>
	
  <route path="/{gtn:sitename}/robots.txt">
    <route-param qname="gtn:handler">
      <value>robots</value>
    </route-param>
  </route>

Note: To ensure your router to be activated, you should paste the configuration above at the top of the "portal access" section of controller.xml file.

3. Config ClearOrphanSymlinksCronJob for removing orphaned symlink
* Open /Jboss_home/server/default/conf/gatein/configuration.properties
* Add new this configuration if not found

#WCM
wcm.linkjob.cron.expression=0 0/20 * * * ?

For further information of expression meaning, please refer to: http://en.wikipedia.org/wiki/CRON_expression

4. Migration mix:votable node type to adapt legacy data with new feature in VoteService:
* Open /Jboss_home/server/default/conf/gatein/configuration.properties
* Add new this configuration if not found

# Commons Upgrade configuration
commons.upgrade.proceedIfFirstRun=true
