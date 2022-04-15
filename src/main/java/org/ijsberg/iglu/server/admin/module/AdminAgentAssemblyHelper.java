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

import org.ijsberg.iglu.access.component.RequestRegistry;
import org.ijsberg.iglu.access.component.StandardAccessManager;
import org.ijsberg.iglu.configuration.Assembly;
import org.ijsberg.iglu.configuration.Cluster;
import org.ijsberg.iglu.configuration.Component;
import org.ijsberg.iglu.configuration.Startable;
import org.ijsberg.iglu.configuration.module.ComponentStarter;
import org.ijsberg.iglu.configuration.module.StandardCluster;
import org.ijsberg.iglu.configuration.module.StandardComponent;
import org.ijsberg.iglu.logging.Logger;
import org.ijsberg.iglu.server.admin.http.AdminAjaxResponseAgent;
import org.ijsberg.iglu.server.facilities.module.UploadAgentImpl;
import org.ijsberg.iglu.server.http.filter.WebAppEntryPoint;
import org.ijsberg.iglu.server.http.module.SimpleJettyServletContext;
import org.ijsberg.iglu.usermanagement.UserManager;
import org.ijsberg.iglu.usermanagement.module.StandardUserManager;
import org.ijsberg.iglu.usermanagement.multitenancy.component.MultiTenantAwareComponent;

import javax.servlet.Filter;

import static org.ijsberg.iglu.util.properties.IgluProperties.loadProperties;

/**
 */
public class AdminAgentAssemblyHelper {
	public static Cluster createAdminLayer(Assembly assembly, Cluster core, Logger logger) {

		//TODO user must replace standard password

		Cluster admin = new StandardCluster();

		Startable componentStarter = new ComponentStarter();
		admin.connect("ComponentStarter", new StandardComponent(componentStarter));

		if(logger != null) {
			admin.connect("Logger", new StandardComponent(logger), Logger.class);
		}
		admin.connect("UploadFactory", new StandardComponent(UploadAgentImpl.getAgentFactory(admin, loadProperties("admin/config/upload_agent.properties"))));

		StandardAccessManager adminAccessManager = new StandardAccessManager(MultiTenantAwareComponent.class);

		Component requestManagerComponent = new StandardComponent(adminAccessManager);
		admin.connect("AdminAccessManager", requestManagerComponent, RequestRegistry.class);

		//make it start & stop
//		admin.connect("AdminAccessManager", requestManagerComponent);
		admin.connect("AdminRequestRegistry", requestManagerComponent, RequestRegistry.class);
		//TODO entry point needs "AccessManager"
		//core.connect("AccessManager", requestManagerComponent);

		UserManager adminUserManager = new StandardUserManager("asdasrasduifS740qadh".getBytes());
		Component adminUserManagerComponent = new StandardComponent(adminUserManager);
		admin.connect("Authenticator", adminUserManagerComponent);
		//TODO properties
		admin.connect("UserManager", adminUserManagerComponent);

		admin.connect("AdminAgentFactory", new StandardComponent(AdminAgentImpl.getAgentFactory(admin)));
		admin.connect("AdminAgentResponseFactory", new StandardComponent(AdminAjaxResponseAgent.getAgentFactory(admin)));

		SimpleJettyServletContext servletContext = new SimpleJettyServletContext();
		Component jettyComponent = new StandardComponent(servletContext);
		jettyComponent.setProperties(loadProperties("admin/config/servlet_context.properties"));
		admin.connect("AdminServletContext", jettyComponent);
		//register as external component
		core.getFacade().connect(jettyComponent);

		for(Filter filter : servletContext.getFilters()) {
			if(filter instanceof WebAppEntryPoint) {
				//by having the entrypoint as a component, we are able to browse it
				//it's the holy grail of CBD
				Component entryPoint = new StandardComponent(filter);
				admin.connect("AdminWebAppEntryPoint", entryPoint);
				//override access manager
				((WebAppEntryPoint)filter).setAccessManager(adminAccessManager);
			}
		}

		core.connect("admin", new StandardComponent(admin));
		//register the assembly itself
		admin.connect("Assembly", new StandardComponent(assembly));

		admin.connect("AdminCluster", new StandardComponent(admin), Cluster.class);
		admin.connect("CoreCluster", new StandardComponent(core));

		componentStarter.start();

		return admin;
	}
}
