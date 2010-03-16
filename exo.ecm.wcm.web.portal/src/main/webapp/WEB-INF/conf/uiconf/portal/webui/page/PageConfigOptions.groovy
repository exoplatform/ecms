import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.core.model.SelectItemCategory;

List categories = new ArrayList(); 

SelectItemCategory appPageConfigs = new SelectItemCategory("WCM Page Configs") ;
categories.add(appPageConfigs) ;
appPageConfigs.addSelectItemOption(new SelectItemOption("SingleContentViewer", "single-content-viewer", "SingleContentViewer")) ;
appPageConfigs.addSelectItemOption(new SelectItemOption("ContentListViewer", "content-list-viewer", "ContentListViewer")) ;
appPageConfigs.addSelectItemOption(new SelectItemOption("Search", "simple-searches-page", "Search")) ;
appPageConfigs.addSelectItemOption(new SelectItemOption("Sitemap", "sitemap-page", "Sitemap")) ;

SelectItemCategory columnPageConfigs = new SelectItemCategory("Column Page Configs") ;
categories.add(columnPageConfigs);  
columnPageConfigs.addSelectItemOption(new SelectItemOption("TwoColumnsLayout", "two-columns", "TwoColumnsLayout"));
columnPageConfigs.addSelectItemOption(new SelectItemOption("ThreeColumnsLayout", "three-columns", "ThreeColumnsLayout"));

SelectItemCategory rowPageConfigs = new SelectItemCategory("Row Page Configs") ;
categories.add(rowPageConfigs); 
rowPageConfigs.addSelectItemOption(new SelectItemOption("TwoRowsLayout", "two-rows", "TwoRowsLayout"));
rowPageConfigs.addSelectItemOption(new SelectItemOption("ThreeRowsLayout", "three-rows", "ThreeRowsLayout"));

SelectItemCategory tabsPageConfigs = new SelectItemCategory("Tabs Page Configs") ;
categories.add(tabsPageConfigs) ;
tabsPageConfigs.addSelectItemOption(new SelectItemOption("TwoTabsLayout", "two-tabs", "TwoTabsLayout")) ;
tabsPageConfigs.addSelectItemOption(new SelectItemOption("ThreeTabsLayout", "three-tabs", "ThreeTabsLayout")) ;

SelectItemCategory mixPageConfigs = new SelectItemCategory("Mix Page Configs") ;
categories.add(mixPageConfigs); 
mixPageConfigs.addSelectItemOption(new SelectItemOption("TwoColumnsOneRowLayout", "two-columns-one-row", "TwoColumnsOneRowLayout"));
mixPageConfigs.addSelectItemOption(new SelectItemOption("OneRowTwoColumnsLayout", "one-row-two-columns", "OneRowTwoColumnsLayout"));
mixPageConfigs.addSelectItemOption(new SelectItemOption("ThreeRowsTwoColumnsLayout", "three-rows-two-columns", "ThreeRowsTwoColumnsLayout"));
return categories;