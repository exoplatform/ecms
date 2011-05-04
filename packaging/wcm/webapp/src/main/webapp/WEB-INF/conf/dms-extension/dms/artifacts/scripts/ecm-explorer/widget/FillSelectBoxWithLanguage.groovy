/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
import java.util.List ;
import java.util.ArrayList ;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;

import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import java.util.Locale;

public class FillSelectBoxWithLanguage implements CmsScript {
	private LocaleConfigService configService;
	public FillSelectBoxWithLanguage(LocaleConfigService localeConfigService) {
	    configService = localeConfigService;
	}  
	  
	public void execute(Object context) {
		UIFormSelectBox selectBox = (UIFormSelectBox) context;
		List options = new ArrayList();	  
	    for (Object object : configService.getLocalConfigs())
	    {
	         LocaleConfig localeConfig = (LocaleConfig)object;
	         Locale locale = localeConfig.getLocale();
	         String lang = locale.getLanguage();
	         String country = locale.getCountry();
		     String optionLang = lang;
		     if (country != null && country.length() > 0) {
		        optionLang += "_" + country;
		     }   
	 		 options.add(new SelectItemOption(optionLang, optionLang));
	 	}          
	    selectBox.setOptions(options);
	}

	public void setParams(String[] params) {  }
}
