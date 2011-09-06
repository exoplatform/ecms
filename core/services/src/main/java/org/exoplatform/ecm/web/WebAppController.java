package org.exoplatform.ecm.web;

import java.io.InputStream;
import org.exoplatform.management.annotations.Impact;
import org.exoplatform.management.annotations.ImpactType;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.web.controller.router.RouterConfigException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class WebAppController extends org.exoplatform.web.WebAppController{
  
	private String configurationPath = "";
	
	public WebAppController(InitParams params) throws Exception {
		super(params);
		// Get router config
    ValueParam routerConfig = params.getValueParam("controller.config");
    if (routerConfig == null)
    {
       throw new IllegalArgumentException("No router param defined");
    }
    configurationPath = routerConfig.getValue();
    reloadConfiguration();
	}
	@Managed
  @ManagedDescription("Load the controller configuration")
  @Impact(ImpactType.WRITE)
	public void loadConfiguration(@ManagedDescription("The configuration path") @ManagedName("path") String path) throws IOException, RouterConfigException {		
		try {
			ConfigurationManager configurationManager = WCMCoreUtils.getService(ConfigurationManager.class);
			InputStream inputStream = configurationManager.getInputStream(configurationPath);
			File file = File.createTempFile("controller-config", ".xml");
			file.deleteOnExit();
			OutputStream out = new FileOutputStream(file);
			int read = 0;
			byte[] bytes = new byte[1024];		 
			while ((read = inputStream.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}		 
			inputStream.close();
			out.flush();
			out.close();
			super.loadConfiguration(file.getAbsolutePath());
		} catch (Exception ex) {		
		}
	}
}
