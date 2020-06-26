package org.exoplatform.services.cms.documents.impl;

import com.sun.star.uno.RuntimeException;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cms.documents.CommonEditorPlugin;

/**
 * The Class CommonEditorPluginImpl is used to share common functions/configuration for editors.
 */
public class CommonEditorPluginImpl extends BaseComponentPlugin implements CommonEditorPlugin {

  /** The Constant DISCOVERY_URL. */
  protected static final String IDLE_TIMEOUT = "idle-timeout";

  /** The idle timeout. */
  protected Long                idleTimeout;

  /**
   * Instantiates a new common editor plugin impl.
   *
   * @param params the params
   */
  public CommonEditorPluginImpl(InitParams params) {
    ValueParam idleTimeoutParam = params.getValueParam(IDLE_TIMEOUT);
    String val = idleTimeoutParam != null ? idleTimeoutParam.getValue() : null;
    if (val == null || (val = val.trim()).isEmpty()) {
      throw new RuntimeException(IDLE_TIMEOUT + " parameter is required.");
    } else {
      this.idleTimeout = Long.valueOf(val);
    }

  }

  /**
   * Gets the idle timeout.
   *
   * @return the idle timeout
   */
  @Override
  public long getIdleTimeout() {
    return idleTimeout;
  }

}
