package org.exoplatform.wcm.notification.plugin;


import java.io.IOException;
import java.io.Writer;
import java.util.*;

import org.exoplatform.social.core.service.LinkProvider;
import org.gatein.common.text.EntityEncoder;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.*;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.DigestTemplate;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.social.notification.Utils;

/**
 * Created by exo on 9/9/16.
 */
@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = ShareFileToUserPlugin.ID, template = "war:/wcm-notification/templates/notification/ShareDocumentToUser.gtmpl"),
    @TemplateConfig(pluginId = ShareFileToSpacePlugin.ID, template = "war:/wcm-notification/templates/notification/ShareDocumentToSpace.gtmpl")
})

public class MailTemplateProvider extends TemplateProvider {

  private static final String SHARE_NOTIFICATION_VEW          = "$UIShareDocuments.label.notification.read";
  private static final String SHARE_NOTIFICATION_MODIFY       = "$UIShareDocuments.label.notification.modify";

  public MailTemplateProvider(InitParams initParams) {
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
      String permission = notification.getValueOwnerParameter(ShareFileToUserPlugin.PERMISSION);


      Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getFrom(), true);
      TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
      String fullName = identity.getProfile().getFullName();
      if(isExternalUser(identity)) {
        fullName += " " + "(" + LinkProvider.getResourceBundleLabel(new Locale(LinkProvider.getCurrentUserLanguage(identity.getRemoteId())), "external.label.tag") + ")";
      }
      templateContext.put("USER", fullName);

      templateContext.put("DOCUMENT", notification.getValueOwnerParameter(ShareFileToUserPlugin.DOCUMENT_NAME));

      templateContext.put("PERMISSION", permission);

      templateContext.put("NOTIF_PERM", permission.equals("read") ? SHARE_NOTIFICATION_VEW : SHARE_NOTIFICATION_MODIFY);

      String subject = TemplateUtils.processSubject(templateContext);

      Identity receiver = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getTo(), true);
      templateContext.put("FIRSTNAME", notification.getValueOwnerParameter(ShareFileToUserPlugin.FIRSTNAME));
      templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
      templateContext.put("DOCUMENT_URL", notification.getValueOwnerParameter(ShareFileToUserPlugin.DOCUMENT_URL));
      templateContext.put("MESSAGE", notification.getValueOwnerParameter(ShareFileToUserPlugin.COMMENT));
      templateContext.put("TYPE", notification.getValueOwnerParameter(ShareFileToUserPlugin.TYPE));

      templateContext.put("THUMBNAIL_URL", notification.getValueOwnerParameter(ShareFileToUserPlugin.DOCUMENT_ICON));
      templateContext.put("FOOTER_LINK", LinkProviderUtils.getRedirectUrl("notification_settings", receiver.getRemoteId()));


      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.subject(subject).body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      EntityEncoder encoder = HTMLEntityEncoder.getInstance();

      List<NotificationInfo> notifications = ctx.getNotificationInfos();
      NotificationInfo first = notifications.get(0);

      String language = getLanguage(first);
      TemplateContext templateContext = new TemplateContext(first.getKey().getId(), language);
      //
      Identity receiver = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, first.getTo(), true);
      templateContext.put("FIRST_NAME", encoder.encode(receiver.getProfile().getProperty(Profile.FIRST_NAME).toString()));
      templateContext.put("FOOTER_LINK", LinkProviderUtils.getRedirectUrl("notification_settings", receiver.getRemoteId()));

      try {
        writer.append(buildDigestMsg(notifications, templateContext));
      } catch (IOException e) {
        ctx.setException(e);
        return false;
      }
      return true;
    }

    protected String buildDigestMsg(List<NotificationInfo> notifications, TemplateContext templateContext) {
      EntityEncoder encoder = HTMLEntityEncoder.getInstance();

      Map<String, List<NotificationInfo>> map = new HashMap<String, List<NotificationInfo>>();
      for (NotificationInfo notif : notifications) {
        String nodeID = notif.getValueOwnerParameter(ShareFileToUserPlugin.NODE_ID);
        List<NotificationInfo> tmp = map.get(nodeID);
        if (tmp == null) {
          tmp = new LinkedList<NotificationInfo>();
          map.put(nodeID, tmp);
        }
        tmp.add(notif);
      }

      StringBuilder sb = new StringBuilder();
      for (String nodeID : map.keySet()) {
        List<NotificationInfo> notifs = map.get(nodeID);
        NotificationInfo first = notifs.get(0);
        String documentUrl = first.getValueOwnerParameter(ShareFileToUserPlugin.DOCUMENT_URL);
        String documentName = first.getValueOwnerParameter(ShareFileToUserPlugin.DOCUMENT_NAME);
        Identity sender = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, first.getFrom(), true);
        templateContext.put("DOCUMENT", buildDocumentUrl(documentUrl, documentName));
        templateContext.put("USER", buildUserUrl(sender));
        templateContext.digestType(DigestTemplate.ElementType.DIGEST_ONE.getValue());

        sb.append("<li style=\"margin: 0 0 13px 14px; font-size: 13px; line-height: 18px; font-family: HelveticaNeue, Helvetica, Arial, sans-serif;\">");
        String digester = TemplateUtils.processDigest(templateContext);
        sb.append(digester);
        sb.append("</div></li>");
      }

      return sb.toString();
    }

    protected String buildUserUrl(Identity identity) {
      String fullName = identity.getProfile().getFullName();
      if(isExternalUser(identity)) {
        fullName += " " + "(" + LinkProvider.getResourceBundleLabel(new Locale(LinkProvider.getCurrentUserLanguage(identity.getRemoteId())), "external.label.tag") + ")";
      }
      StringBuilder sb = new StringBuilder();
      sb.append("<a target=\"_blank\" style=\"text-decoration: none; font-weight: bold; color: #2f5e92; font-family: 'HelveticaNeue Bold', Helvetica, Arial, sans-serif; font-size: 13px; line-height: 18px;\"");
      sb.append("href=\"" + LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()) + "\">");
      sb.append(fullName);
      sb.append("</a>");
      return sb.toString();
    }

    protected String buildDocumentUrl(String docUrl, String docName) {
      return "<a target=\"_blank\" style=\"text-decoration: none; font-weight: bold; color: #2f5e92; font-family: 'HelveticaNeue Bold', Helvetica, Arial, sans-serif; font-size: 13px; line-height: 18px;\" href=\"" + docUrl + "\">" + docName + "</a>";
    }
  };

  /** Defines the template builder for ShareFileToSpacePlugin*/
  private AbstractTemplateBuilder shareFileToSpacePlugin = new AbstractTemplateBuilder() {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();
      String language = getLanguage(notification);
      String permission = notification.getValueOwnerParameter(ShareFileToUserPlugin.PERMISSION);

      Identity identity = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getFrom(), true);
      TemplateContext templateContext = new TemplateContext(notification.getKey().getId(), language);
      String fullName = identity.getProfile().getFullName();
      if(isExternalUser(identity)) {
        fullName += " " + "(" + LinkProvider.getResourceBundleLabel(new Locale(LinkProvider.getCurrentUserLanguage(identity.getRemoteId())), "external.label.tag") + ")";
      }
      templateContext.put("USER", fullName);

      templateContext.put("DOCUMENT", notification.getValueOwnerParameter(ShareFileToSpacePlugin.DOCUMENT_NAME));

      templateContext.put("PERMISSION", notification.getValueOwnerParameter(ShareFileToSpacePlugin.PERMISSION));

      templateContext.put("NOTIF_PERM", permission.equals("read") ? SHARE_NOTIFICATION_VEW : SHARE_NOTIFICATION_MODIFY);

      String subject = TemplateUtils.processSubject(templateContext);

      Identity receiver = Utils.getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getTo(), true);
      templateContext.put("FIRSTNAME", ShareFileToSpacePlugin.capitalizeFirstLetter(notification.getTo()));
      templateContext.put("PROFILE_URL", LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()));
      templateContext.put("DOCUMENT_URL", notification.getValueOwnerParameter(ShareFileToSpacePlugin.DOCUMENT_URL));
      templateContext.put("MESSAGE", notification.getValueOwnerParameter(ShareFileToSpacePlugin.COMMENT));
      templateContext.put("SPACE", notification.getValueOwnerParameter(ShareFileToSpacePlugin.SPACE_NAME));
      templateContext.put("SPACE_URL", notification.getValueOwnerParameter(ShareFileToSpacePlugin.SPACE_URL));
      templateContext.put("TYPE", notification.getValueOwnerParameter(ShareFileToSpacePlugin.TYPE));
      templateContext.put("REPLY_ACTION_URL", notification.getValueOwnerParameter(ShareFileToSpacePlugin.REPLY));
      templateContext.put("VIEW_FULL_DISCUSSION_ACTION_URL", notification.getValueOwnerParameter(ShareFileToSpacePlugin.FULL_DISUSSION));

      templateContext.put("THUMBNAIL_URL", notification.getValueOwnerParameter(ShareFileToSpacePlugin.DOCUMENT_ICON));
      templateContext.put("FOOTER_LINK", LinkProviderUtils.getRedirectUrl("notification_settings", receiver.getRemoteId()));


      String body = TemplateUtils.processGroovy(templateContext);
      //binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.subject(subject).body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      EntityEncoder encoder = HTMLEntityEncoder.getInstance();

      List<NotificationInfo> notifications = ctx.getNotificationInfos();
      NotificationInfo first = notifications.get(0);

      String language = getLanguage(first);
      TemplateContext templateContext = new TemplateContext(first.getKey().getId(), language);
      //
      Identity receiver = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, first.getTo(), true);
      templateContext.put("FIRST_NAME", encoder.encode(receiver.getProfile().getProperty(Profile.FIRST_NAME).toString()));
      templateContext.put("FOOTER_LINK", LinkProviderUtils.getRedirectUrl("notification_settings", receiver.getRemoteId()));

      try {
        writer.append(buildDigestMsg(notifications, templateContext));
      } catch (IOException e) {
        ctx.setException(e);
        return false;
      }
      return true;
    }
    protected String buildDigestMsg(List<NotificationInfo> notifications, TemplateContext templateContext) {
      EntityEncoder encoder = HTMLEntityEncoder.getInstance();

      Map<String, List<NotificationInfo>> map = new HashMap<String, List<NotificationInfo>>();
      for (NotificationInfo notif : notifications) {
        String nodeID = notif.getValueOwnerParameter(ShareFileToSpacePlugin.NODE_ID);
        List<NotificationInfo> tmp = map.get(nodeID);
        if (tmp == null) {
          tmp = new LinkedList<NotificationInfo>();
          map.put(nodeID, tmp);
        }
        tmp.add(notif);
      }

      StringBuilder sb = new StringBuilder();
      for (String nodeID : map.keySet()) {
        List<NotificationInfo> notifs = map.get(nodeID);
        NotificationInfo first = notifs.get(0);
        String documentUrl = first.getValueOwnerParameter(ShareFileToSpacePlugin.DOCUMENT_URL);
        String documentName = first.getValueOwnerParameter(ShareFileToSpacePlugin.DOCUMENT_NAME);
        Identity sender = CommonsUtils.getService(IdentityManager.class).getOrCreateIdentity(OrganizationIdentityProvider.NAME, first.getFrom(), true);
        templateContext.put("DOCUMENT", buildDocumentUrl(documentUrl, documentName));
        templateContext.put("USER", buildUserUrl(sender));
        templateContext.digestType(DigestTemplate.ElementType.DIGEST_ONE.getValue());

        sb.append("<li style=\"margin: 0 0 13px 14px; font-size: 13px; line-height: 18px; font-family: HelveticaNeue, Helvetica, Arial, sans-serif;\">");
        String digester = TemplateUtils.processDigest(templateContext);
        sb.append(digester);
        sb.append("</div></li>");
      }

      return sb.toString();
    }

    protected String buildUserUrl(Identity identity) {
      String fullName = identity.getProfile().getFullName();
      if(isExternalUser(identity)) {
        fullName += " " + "(" + LinkProvider.getResourceBundleLabel(new Locale(LinkProvider.getCurrentUserLanguage(identity.getRemoteId())), "external.label.tag") + ")";
      }
      StringBuilder sb = new StringBuilder();
      sb.append("<a target=\"_blank\" style=\"text-decoration: none; font-weight: bold; color: #2f5e92; font-family: 'HelveticaNeue Bold', Helvetica, Arial, sans-serif; font-size: 13px; line-height: 18px;\"");
      sb.append("href=\"" + LinkProviderUtils.getRedirectUrl("user", identity.getRemoteId()) + "\">");
      sb.append(fullName);
      sb.append("</a>");
      return sb.toString();
    }

    protected String buildDocumentUrl(String docUrl, String docName) {
      return "<a target=\"_blank\" style=\"text-decoration: none; font-weight: bold; color: #2f5e92; font-family: 'HelveticaNeue Bold', Helvetica, Arial, sans-serif; font-size: 13px; line-height: 18px;\" href=\"" + docUrl + "\">" + docName + "</a>";
    }

  };

  private boolean isExternalUser(Identity identity) {
    return identity.getProfile() != null && identity.getProfile().getProperty("external") != null && identity.getProfile().getProperty("external").equals("true");
  }
}
