/*
 * Copyright 2011-2014 Jeroen Meetsma - IJsberg Automatisering BV
 *
 * This file is part of Iglu.
 *
 * Iglu is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Iglu is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Iglu.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ijsberg.iglu.server.admin.module;

import org.ijsberg.iglu.access.*;
import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.http.widget.MenuItem;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.logging.Logger;
import org.ijsberg.iglu.server.admin.AdminAgent;
import org.ijsberg.iglu.util.collection.CollectionSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


/**
 */
public class AdminAgentImpl implements AdminAgent, SessionDestructionListener, Logger {

    public static final String ADMIN_AGENT_NAME = "AdminAgent";
    private RequestRegistry requestRegistry;
    private Session session;
	private Logger logger;
	private Logger loggerAppender;
	private static int MAX_LOG_SIZE = 1000;

    public static AgentFactory<AdminAgent> getAgentFactory(Cluster cluster) {
        return new BasicAgentFactory<AdminAgent>(cluster, ADMIN_AGENT_NAME) {
            public AdminAgent createAgentImpl() {
                return new AdminAgentImpl();
            }
            public Cluster getCluster() {
            	return cluster;
			}
        };
    }

	/**
	 * Invoked by injection.
	 */
	public void setLogger(Logger logger) {

//		System.out.println("LOGGER set");
//		if(logger instanceof SimpleFileLogger) {
			this.logger = logger;
//			System.out.println("appender added");
			this.logger.addAppender(this);
//		}
	}


    /**
     * Invoked by injection.
     * @param requestRegistry
     */
    public void setAdminAccessManager(RequestRegistry requestRegistry) {

		session = requestRegistry.getCurrentRequest().getSession(true);
	}


    @Override
    public boolean login(String userName, String password) {
        try {
            session.login(new SimpleCredentials(userName, password));
        } catch (AuthenticationException e) {
            //TODO return reason
            return false;
        }
        return session.getUser() != null;
    }

    @Override
    public boolean isLoggedIn() {
        return session.getUser() != null;
    }

	@Override
	public String getMenu() {
		String jsonTree =
				"[" + CollectionSupport.format(getMenuItems(), ",\n") + "\n];\n";
		return jsonTree;
	}



	public List<MenuItem> getMenuItems() {

		List<MenuItem> menu = new ArrayList<MenuItem>();

		menu.add(new MenuItem("options", "options").addSubmenu(getOptionsMenu()).addCssClassNames("top_menu_item", "submenu"));
		menu.add(new MenuItem("system", "system").addSubmenu(getSytemMenu()).addCssClassNames("top_menu_item", "submenu"));

/*		menu.add(new MenuItem("upload", "directory structure").addLink("code/directories.js", "navigation", "directory structure").addCssClassName("top_menu_item"));

		List<MenuItem> duplicationMenu = getDuplicationMenu();
		menu.add(new MenuItem("duplication", "duplication").addSubmenu(duplicationMenu).addCssClassNames("top_menu_item", "submenu"));

		List<MenuItem> fileMetricsMenu = getReportMetricsMenu(analysis.getReportFileMetrics(), "file");
		menu.add(new MenuItem("file_metrics", "file metrics").addSubmenu(fileMetricsMenu).addCssClassNames("top_menu_item", "submenu"));

		List<MenuItem> unitMetricsMenu = getReportMetricsMenu(analysis.getReportUnitMetrics(), "function");
		menu.add(new MenuItem("function_metrics", "function metrics").addSubmenu(unitMetricsMenu).addCssClassNames("top_menu_item", "submenu"));       */

		return menu;
	}

	private List<MenuItem> getOptionsMenu() {
		List<MenuItem> optionsMenu = new ArrayList<MenuItem>();
		//optionsMenu.add(new MenuItem("js_log", "javascript log").addOnclick("openJavaScriptLog()").addCssClassName("submenu_item"));
		optionsMenu.add(new MenuItem("server_log", "server log").addOnclick("openServerLog()").addCssClassName("submenu_item"));
/*		optionsMenu.add(new MenuItem("style", "toggle resolved references").addOnclick("toggleCss(4);").addCssClassName("submenu_item"));    */
		return optionsMenu;
	}

	private List<MenuItem> getSytemMenu() {
		List<MenuItem> optionsMenu = new ArrayList<MenuItem>();
		optionsMenu.add(new MenuItem("update", "update").addOnclick("openUpdateWindow()").addCssClassName("submenu_item"));
		return optionsMenu;
	}

	@Override
	public void onSessionDestruction() {
		this.logger.removeAppender(this);
	}

	private final List<String> logEntries = new ArrayList<String>();

	@Override
	public void log(LogEntry entry) {
		String logEntryString = entry.toString();
			if(!logEntryString.contains("getLogEntries")) {
			logEntryString = logEntryString.replaceAll("\"", "&quot;");
			logEntryString = logEntryString.replaceAll("'", " &lsquo;");
			synchronized (logEntries) {
				logEntries.add(logEntryString);
				if(logEntries.size() > MAX_LOG_SIZE) {
					logEntries.remove(0);
				}
			}
		}
	}

    @Override
	public void addAppender(Logger appender) {

	}

	@Override
	public void removeAppender(Logger appender) {

	}

	@Override
	public int getLogLevelOrdinal() {
		return logger.getLogLevelOrdinal();
	}

	@Override
	public Properties getProperties() {
		return null;
	}

	@Override
	public String getLogEntries() {
		String jsonString = null;
		synchronized (logEntries) {
			jsonString =
				"[" + CollectionSupport.format("'", logEntries, "'", ",\n") + "\n];\n";
			logEntries.clear();
		}
		return jsonString;
	}
}
