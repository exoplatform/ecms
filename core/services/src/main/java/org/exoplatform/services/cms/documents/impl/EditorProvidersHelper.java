package org.exoplatform.services.cms.documents.impl;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.exoplatform.services.cms.documents.DocumentEditorProvider;
import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.webui.application.WebuiRequestContext;

/**
 * The Class EditorProviderHelper.
 */
public class EditorProvidersHelper {
  /** The Constant LOG. */
  protected static final Log           LOG = ExoLogger.getLogger(EditorProvidersHelper.class);

  /** The document service. */
  private static EditorProvidersHelper helper;

  /** The document service. */
  private DocumentService              documentService;

  /**
   * Instantiates a new editor provider helper.
   *
   * @param documentService the document service
   */
  private EditorProvidersHelper(DocumentService documentService) {
    this.documentService = documentService;
  }

  /**
   * Inits the explorer.
   *
   * @param providers the providers
   * @param identity the identity
   * @param fileId the file id
   * @param workspace the workspace
   * @param context the context
   * @return the list
   */
  public List<ProviderInfo> initExplorer(List<DocumentEditorProvider> providers,
                                         Identity identity,
                                         String fileId,
                                         String workspace,
                                         WebuiRequestContext context) {
    return initEditorProviders(providers,
                               identity,
                               fileId,
                               workspace,
                               (provider) -> provider.initExplorer(fileId, workspace, context));

  }

  /**
   * Inits the preview.
   *
   * @param providers the providers
   * @param identity the identity
   * @param fileId the file id
   * @param workspace the workspace
   * @param requestUri the request uri
   * @param locale the locale
   * @return the list
   */
  public List<ProviderInfo> initPreview(List<DocumentEditorProvider> providers,
                                        Identity identity,
                                        String fileId,
                                        String workspace,
                                        URI requestUri,
                                        Locale locale) {
    return initEditorProviders(providers,
                               identity,
                               fileId,
                               workspace,
                               (provider) -> provider.initPreview(fileId, workspace, requestUri, locale));
  }

  /**
   * Gets the single instance of EditorProviderHelper.
   *
   * @return single instance of EditorProviderHelper
   */
  public static EditorProvidersHelper getInstance() {
    return helper;
  }

  /**
   * Inits the editor providers.
   *
   * @param providers the providers
   * @param identity the identity
   * @param fileId the file id
   * @param workspace the workspace
   * @param initFunction the init function
   * @return the list
   */
  protected List<ProviderInfo> initEditorProviders(List<DocumentEditorProvider> providers,
                                                   Identity identity,
                                                   String fileId,
                                                   String workspace,
                                                   Function<DocumentEditorProvider, Object> initFunction) {
    String preferredProvider = getPrefferedEditor(identity.getUserId(), fileId, workspace);
    String currentProvider = getCurrentEditor(fileId, workspace);
    List<ProviderInfo> providersInfo = providers.stream()
                                                .filter(provider -> provider.isAvailableForUser(identity))
                                                .map(provider -> {
                                                  try {
                                                    Object editorSettings = initFunction.apply(provider);
                                                    boolean preffered = provider.getProviderName().equals(preferredProvider);
                                                    boolean current = provider.getProviderName().equals(currentProvider);
                                                    return new ProviderInfo(provider.getProviderName(),
                                                                            editorSettings,
                                                                            preffered,
                                                                            current);
                                                  } catch (Exception e) {
                                                    LOG.error("Cannot init provider " + provider.getProviderName(), e);
                                                    return null;
                                                  }
                                                })
                                                .filter(providerInfo -> providerInfo != null)
                                                .collect(Collectors.toList());

    return providersInfo;
  }

  /**
   * Inits the EditorProvidersHelper.
   *
   * @param documentService the document service
   */
  protected static void init(DocumentService documentService) {
    if (helper == null) {
      helper = new EditorProvidersHelper(documentService);
    } else {
      LOG.warn("EditorProviderHelper is already initialized");
    }
  }

  /**
   * Gets the preffered editor.
   *
   * @param userId the user id
   * @param fileId the file id
   * @param workspace the workspace
   * @return the preffered editor
   */
  protected String getPrefferedEditor(String userId, String fileId, String workspace) {
    String prefferedProvider = null;
    try {
      prefferedProvider = documentService.getPreferredEditor(userId, fileId, workspace);
    } catch (RepositoryException e) {
      LOG.error("Cannot get preffered editor for fileId " + fileId, e);
    }
    return prefferedProvider;
  }

  /**
   * Gets the current editor.
   *
   * @param fileId the file id
   * @param workspace the workspace
   * @return the current editor
   */
  protected String getCurrentEditor(String fileId, String workspace) {
    String currentProvider = null;
    try {
      currentProvider = documentService.getCurrentDocumentProvider(fileId, workspace);
    } catch (RepositoryException e) {
      LOG.error("Cannot get current editor for fileId " + fileId, e);
    }
    return currentProvider;
  }

  /**
   * The Class ProviderInfo.
   */
  public static class ProviderInfo {

    /** The provider. */
    private final String  provider;

    /** The settings. */
    private final Object  settings;

    /** The is preffered. */
    private final boolean isPreferred;

    /** The is current. */
    private final boolean isCurrent;

    /**
     * Instantiates a new provider info.
     *
     * @param provider the provider
     * @param settings the settings
     * @param isPreferred the isPreferred
     * @param isCurrent the is current
     */
    public ProviderInfo(String provider, Object settings, boolean isPreferred, boolean isCurrent) {
      this.provider = provider;
      this.settings = settings;
      this.isPreferred = isPreferred;
      this.isCurrent = isCurrent;
    }

    /**
     * Gets the provider.
     *
     * @return the provider
     */
    public String getProvider() {
      return provider;
    }

    /**
     * Gets the settings.
     *
     * @return the settings
     */
    public Object getSettings() {
      return settings;
    }

    
    /**
     * Checks if is preffered.
     *
     * @return true, if is preffered
     */
    public boolean isPreferred() {
      return isPreferred;
    }

    /**
     * Checks if is current.
     *
     * @return true, if is current
     */
    public boolean isCurrent() {
      return isCurrent;
    }
  }

}
