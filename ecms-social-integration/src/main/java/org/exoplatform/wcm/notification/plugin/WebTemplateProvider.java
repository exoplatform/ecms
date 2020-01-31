package org.exoplatform.wcm.notification.plugin;

import java.io.Writer;
import java.util.Calendar;
import java.util.Locale;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.NotificationMessageUtils;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.*;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.notification.Utils;
import org.exoplatform.webui.utils.TimeConvertUtils;

/**
 * Created by exo on 9/9/16.
 */
@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = ShareFileToUserPlugin.ID, template = "war:/groovy/ecm/social-integration/plugin/intranet-notification/ShareDocumentToUser.gtmpl"),
    @TemplateConfig(pluginId = ShareFileToSpacePlugin.ID, template = "war:/groovy/ecm/social-integration/plugin/intranet-notification/ShareDocumentToSpace.gtmpl")
})

public class WebTemplateProvider extends TemplateProvider {

  public WebTemplateProvider(InitParams initParams) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(ShareFileToUserPlugin.ID), shareFileToUserPlugin);
    this.templateBuilders.put(PluginKey.key(ShareFileToSpacePlugin.ID), shareFileToSpacePlugin);

  }

  /** Defines the template builder for ShareFileToUserPlugin*/
  private AbstractTemplateBuilder shareFileToUserPlugin = new AbstractTemplateBuilder() {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);

      Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getFrom(), true);
      Profile profile = identity.getProfile();

      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());

      templateContext.put("THUMBNAIL_URL", notification.getValueOwnerParameter(ShareFileToUserPlugin.DOCUMENT_ICON));
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("USER", profile.getFullName());
      templateContext.put("AVATAR", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      templateContext.put("DOCUMENT", notification.getValueOwnerParameter(ShareFileToUserPlugin.DOCUMENT_NAME));
      templateContext.put("DOCUMENT_URL", notification.getValueOwnerParameter(ShareFileToUserPlugin.DOCUMENT_URL));
      templateContext.put("PERMISSION", notification.getValueOwnerParameter(ShareFileToUserPlugin.PERMISSION));
      templateContext.put("MESSAGE", notification.getValueOwnerParameter(ShareFileToUserPlugin.COMMENT));

      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }


  };

  /** Defines the template builder for ShareFileToSpacePlugin*/
  private AbstractTemplateBuilder shareFileToSpacePlugin = new AbstractTemplateBuilder() {

    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), notification.getKey().getId(), language);

      Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getFrom(), true);
      Profile profile = identity.getProfile();

      templateContext.put("isIntranet", "true");
      Calendar cal = Calendar.getInstance();
      cal.setTimeInMillis(notification.getLastModifiedDate());

      templateContext.put("THUMBNAIL_URL", notification.getValueOwnerParameter(ShareFileToSpacePlugin.DOCUMENT_ICON));
      templateContext.put("READ", Boolean.valueOf(notification.getValueOwnerParameter(NotificationMessageUtils.READ_PORPERTY.getKey())) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      templateContext.put("LAST_UPDATED_TIME", TimeConvertUtils.convertXTimeAgoByTimeServer(cal.getTime(), "EE, dd yyyy", new Locale(language), TimeConvertUtils.YEAR));
      templateContext.put("USER", profile.getFullName());
      templateContext.put("AVATAR", profile.getAvatarUrl() != null ? profile.getAvatarUrl() : LinkProvider.PROFILE_DEFAULT_AVATAR_URL);
      templateContext.put("DOCUMENT", notification.getValueOwnerParameter(ShareFileToSpacePlugin.DOCUMENT_NAME));
      templateContext.put("DOCUMENT_URL", notification.getValueOwnerParameter(ShareFileToSpacePlugin.DOCUMENT_URL));
      templateContext.put("SPACE", notification.getValueOwnerParameter(ShareFileToSpacePlugin.SPACE_NAME));
      templateContext.put("PERMISSION", notification.getValueOwnerParameter(ShareFileToSpacePlugin.PERMISSION));
      templateContext.put("MESSAGE", notification.getValueOwnerParameter(ShareFileToSpacePlugin.COMMENT));

      //
      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }


  };

}