/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.myfaces.examples.misc;

import org.apache.myfaces.custom.navmenu.NavigationMenuItem;
import org.apache.myfaces.custom.navmenu.jscookmenu.HtmlCommandJSCookMenu;
import org.apache.myfaces.custom.navmenu.htmlnavmenu.HtmlCommandNavigationItem;
import org.apache.myfaces.examples.util.GuiUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.event.ActionEvent;
import java.util.List;
import java.util.ArrayList;

/**
 * @author Thomas Spiegl (latest modification by $Author: tomsp $)
 * @version $Revision: 419187 $ $Date: 2006-07-05 04:11:08 -0400 (Wed, 05 Jul 2006) $
 */
public class NavigationMenu {
    private static final Log log = LogFactory.getLog(NavigationMenu.class);

    public NavigationMenuItem[] getInfoItems() {
        String label = GuiUtil.getMessageResource("nav_Info", null);
        NavigationMenuItem[] menu = new NavigationMenuItem[1];

        menu[0] = new NavigationMenuItem(label, null, null, true);

        NavigationMenuItem[] items = new NavigationMenuItem[2];
        menu[0].setNavigationMenuItems(items);

        label = GuiUtil.getMessageResource("nav_Contact", null);
        items[0] = new NavigationMenuItem(label, "go_contact", "images/help.gif", false);

        label = GuiUtil.getMessageResource("nav_Copyright", null);
        items[1] = new NavigationMenuItem(label, "go_copyright", "images/help.gif", false);

        return menu;
    }

    public List getJSCookMenuNavigationItems() {
        List menu = new ArrayList();
        menu.add(getMenuNaviagtionItem("Home", "go_home"));
        return menu;
    }

    public List getPanelNavigationItems() {
        List menu = new ArrayList();
        // Products
        NavigationMenuItem products = getMenuNaviagtionItem("#{example_messages['panelnav_products']}", null);
        menu.add(products);
        products.add(getMenuNaviagtionItem("#{example_messages['panelnav_serach']}", "#{navigationMenu.getAction2}"));
        products.add(getMenuNaviagtionItem("#{example_messages['panelnav_serach_acc']}", "#{navigationMenu.getAction2}"));
        NavigationMenuItem item = getMenuNaviagtionItem("#{example_messages['panelnav_search_adv']}", "#{navigationMenu.getAction2}");
        item.setActive(true);
        item.setOpen(true);
        item.setTarget("_blank");
        products.add(item);
        // Shop
        menu.add(getMenuNaviagtionItem("#{example_messages['panelnav_shop']}", "#{navigationMenu.getAction2}"));
        // Corporate Info
        NavigationMenuItem corporateInfo = getMenuNaviagtionItem("#{example_messages['panelnav_corporate']}", null);
        menu.add(corporateInfo);
        corporateInfo.add(getMenuNaviagtionItem("#{example_messages['panelnav_news']}", "#{navigationMenu.getAction2}"));
        item = getMenuNaviagtionItem("#{example_messages['panelnav_investor']}", "#{navigationMenu.getAction3}");
        //item.setIcon("images/arrow-first.gif");
        item.setDisabled(true);
        corporateInfo.add(item);
        // Contact
        menu.add(getMenuNaviagtionItem("#{example_messages['panelnav_contact']}", "#{navigationMenu.getAction2}"));
        // External Link
        item = getMenuNaviagtionItem("#{example_messages['panelnav_contact']}", null);
        item.setExternalLink("#{example_messages['external_link']}");
        item.setTarget("_blank");
        menu.add(item);
        return menu;
    }

    private static NavigationMenuItem getMenuNaviagtionItem(String label, String action) {
        NavigationMenuItem item = new NavigationMenuItem(label, action);
        item.setActionListener("#{navigationMenu.actionListener}");
        item.setValue(label);
        return item;
    }

    public String getAction1() {
        return "go_panelnavigation_1";
    }

    public String actionListener(ActionEvent event) {
        if (event.getComponent() instanceof HtmlCommandNavigationItem) {
            log.info("ActionListener: " + ((HtmlCommandNavigationItem) event.getComponent()).getValue());
            return getAction1();
        }
        else {
            String outcome = (String) ((HtmlCommandJSCookMenu) event.getComponent()).getValue();
            log.info("ActionListener: " + outcome);
            return outcome;
        }
    }

    public String getAction2() {
        return "go_panelnavigation_2";
    }

    public String getAction3() {
        return "go_panelnavigation_3";
    }

    public String goHome() {
        return "go_home";
    }

    public boolean getDisabled() {
        return true;
    }
}
