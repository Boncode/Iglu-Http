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

package org.ijsberg.iglu.server.http.filter;

import org.ijsberg.iglu.access.*;
import org.ijsberg.iglu.configuration.ConfigurationException;
import org.ijsberg.iglu.http.client.AuthorizationBearer;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.server.http.servlet.ServletRequestAlreadyRedirectedException;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.formatting.PatternMatchingSupport;
import org.ijsberg.iglu.util.http.ServletSupport;
import org.ijsberg.iglu.util.misc.EncodingSupport;
import org.ijsberg.iglu.util.misc.StringSupport;
import org.ijsberg.iglu.util.properties.IgluProperties;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;


/**
 * This filter can be used as point of entry into a web application.
 * This filter performs several tasks:
 * <li>It binds a request as soon as a http-request enters the system.</li>
 * <li>In the process it may also resolve a related session, referenced by a session token (stored client-side).</li>
 * <li>It releases the request as soon as the http-request has been processed.</li>
 * <li>It may perform role based access control.</li>
 * <li>It provides exception handling.</li>
 * Servlets and JSP-pages that are part of the web application will thus
 * be provided with a registered request that can be retrieved throughout the course of
 * the request by invoking <emph>application.getCurrentRequest()</emph>.
 * <p/>
 * Specify a servlet init parameter 'realm_id=....' to indicate which realm is entered here.
 * <p/>
 * Make sure to filter on dynamic content only, to avoid unnecessary overhead of request administration.
 *
 * If an URL-pattern matches multiple filters, the request will pass these
 * filters in a chain. Note that the first filter creates the request and
 * determines which realm is entered.  
 */
public class WebAppEntryPoint implements Filter, EntryPoint
{
	private String xorKey;

	private AccessManager accessManager;

	public static final String SESSION_TOKEN_KEY = "IGLU_SESSION_TOKEN";
	private String USER_ID_KEY = "IGLU_USER_ID";

	protected String filterName;

	private boolean syncUserPrefs;
	private int userPrefsMaxAge = -1;

	private HashMap exceptionPages = new HashMap();
//	private List exceptionsHandled = new ArrayList();
	//debug mode
	private boolean printUnhandledExceptions = true;

	private ThreadLocal httpRequest = new ThreadLocal();
	private ThreadLocal httpResponse = new ThreadLocal();

	private HashMap securityConstraints = new HashMap();


	private boolean loggingEnabled;
	private String exceptionPagesSectionId;

	private boolean loginRequired = false;
	private String loginPath;

	private String publicContentRegExp;
	private String staticContentRegExp;

	private boolean passSessionIdSecure = false;

	private IgluProperties additionalHeaders = new IgluProperties();
	private IgluProperties additionalStaticContentHeaders = new IgluProperties();


	private static class ExceptionHandlingSettings {
		public String redirectPage;
		public int loglevel;


		public ExceptionHandlingSettings(String redirectPage, int loglevel) {
			this.redirectPage = redirectPage;
			this.loglevel = loglevel;
		}
	}


	public void setAccessManager(AccessManager accessManager) {
		this.accessManager = accessManager;
	}

	public void setXorKey(String xorKey) {
		this.xorKey = xorKey;
	}

	/**
     *
     */
    public WebAppEntryPoint()
	{
		super();
	}

	/**
	 * Notification of a session registration event.
	 *
	 * @param session
	 */
	public void onSessionUpdate(Request currentRequest, Session session) {
		HttpServletResponse response = (HttpServletResponse)httpResponse.get();
		storeSessionDataInCookie(SESSION_TOKEN_KEY, session.getToken(), response);
		if(session.getUser() != null) {
			storeSessionDataInCookie(USER_ID_KEY, session.getUser().getId(), response);
		}
		else {
			storeSessionDataInCookie(USER_ID_KEY, null, response);
		}
	}

	/**
	 * Notification of a session destruction.
	 *
	 * @param session
	 */
	public void onSessionDestruction(Request currentRequest, Session session) {
		HttpServletResponse response = (HttpServletResponse)httpResponse.get();
		storeSessionDataInCookie(SESSION_TOKEN_KEY, null, response);
		storeSessionDataInCookie(USER_ID_KEY, null, response);
	}

    /**
     *
     * @param currentRequest
     * @param properties
     */
    public void exportUserSettings(Request currentRequest, Properties properties) {
		HttpServletResponse response = (HttpServletResponse)httpResponse.get();
		ServletSupport.exportCookieValues(response, properties, "/", userPrefsMaxAge);
	}


	/**
	 * Stores session id in the root of a cookie
	 * The cookie will expire as soon as the browser closes
	 *
	 * @param key
	 * @param value
	 * @param response
	 */
	public void storeSessionDataInCookie(String key, String value, ServletResponse response) {
		Cookie cookie = new Cookie(key, value);
		cookie.setPath("/");
		cookie.setHttpOnly(true);
		cookie.setSecure(passSessionIdSecure);
		cookie.setMaxAge(-1);//expire when browser closes
		((HttpServletResponse)response).addCookie(cookie);
	}

    /**
     *
     * @param currentRequest
     * @param properties
     */
    public void importUserSettings(Request currentRequest, Properties properties) {
		ServletRequest request = (ServletRequest)httpRequest.get();
		ServletSupport.importCookieValues(request, properties);
	}


	//TODO document init params
	/**
	 * @param conf
	 * @throws
	 */
	public final void init(FilterConfig conf) {
		filterName = conf.getFilterName();

		String syncUserPrefsStr = conf.getInitParameter("sync_user_prefs");
		syncUserPrefs = (syncUserPrefsStr != null ? Boolean.valueOf(syncUserPrefsStr) : false);

		String loginRequiredStr = conf.getInitParameter("login_required");
		loginRequired = (loginRequiredStr != null ? Boolean.valueOf(loginRequiredStr) : false);
		loginPath = conf.getInitParameter("login_path");

		//pass_session_id_secure
		String passSessionIdSecureStr = conf.getInitParameter("pass_session_id_secure");
		passSessionIdSecure = (passSessionIdSecureStr != null ? Boolean.valueOf(passSessionIdSecureStr) : false);

		gatherAdditionalHeaders(conf);

		System.out.println(new LogEntry("loginRequired:" + loginRequired));

		String userPrefsMaxAgeStr = conf.getInitParameter("user_prefs_max_age");
		userPrefsMaxAge = userPrefsMaxAgeStr != null ? Integer.valueOf(userPrefsMaxAgeStr) : userPrefsMaxAge;

		publicContentRegExp = conf.getInitParameter("public_content_reg_exp");
		staticContentRegExp = conf.getInitParameter("static_content_reg_exp");
	}

	private void gatherAdditionalHeaders(FilterConfig conf) {
		Enumeration initParamNames = conf.getInitParameterNames();
		while(initParamNames.hasMoreElements()) {
			String initParamName = (String)initParamNames.nextElement();
			if(initParamName.startsWith("header.")) {
				additionalHeaders.setProperty(initParamName.substring(7), conf.getInitParameter(initParamName));
			}
			if(initParamName.startsWith("static_content_header.")) {
				additionalStaticContentHeaders.setProperty(initParamName.substring("static_content_header.".length()), conf.getInitParameter(initParamName));
			}
			//additionalStaticContentHeaders
			//static_content_header
		}
	}

/*	private void initializeExceptionPages()
			throws ServletException
	{
		PropertyBundle exceptionPagesSection = application.getConfigurationSection(exceptionPagesSectionId);
		exceptionPagesSection.setDescription("declares handling in the form of:\n" +
				"remainingexceptions.class=java.lang.Exception\n" +
				"remainingexceptions.page=error.jsp\n" +
				"remainingexceptions.loglevel=CRITICAL\n" +
				"Note: order matters");
//		if(exceptionPagesSection != null)
//	{
		Iterator defIterator = exceptionPagesSection.getSubsectionKeys().iterator();
		while(defIterator.hasNext())
		{
			String key = (String)defIterator.next();
//			PropertyBundle def = (PropertyBundle)defIterator.next();
			String className = exceptionPagesSection.getValue(key + ".class", "org.ijsberg.iglu.SomeException").toString();
			Class exceptionClass;
			try
			{
				exceptionClass = Class.forName(className);
			}
			catch(Throwable t)
			{
				throw new ServletException("filter " + filterName + " can not handle exception '" + className + "'", t);
			}
			exceptionPages.put(exceptionClass, new ExceptionHandlingSettings(
					exceptionPagesSection.getValue(key + ".page", "error.html").toString(),
					exceptionPagesSection.getIndex(key + ".loglevel", StandardApplication.levelString, Application.CRITICAL)));


		}
//	}
	}*/



	/**
	 * Must handle all incoming http requests.
	 * Contains hooks for request and session management.
	 *
	 * @param servletRequest
	 * @param servletResponse
	 * @throws IOException
	 */
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws ServletException, IOException {

		//TODO IP addresses can be obtained when request is passed by Apache (reverse proxy) from header X-Forwarded-For
		//TODO IP blocking should be added

		for(String header : additionalHeaders.toOrderedMap().keySet()) {
			String additionalHeaderValue = additionalHeaders.getProperty(header);
			if(isStaticContent(getPath(servletRequest))) {
				//System.out.println(getPath(servletRequest) + " MATCHES " + staticContentRegExp);
				if(additionalStaticContentHeaders.containsKey(header)) {
					additionalHeaderValue = additionalStaticContentHeaders.getProperty(header);
				}
			}
			((HttpServletResponse)servletResponse).addHeader(header, additionalHeaderValue);
		}

		servletRequest.setCharacterEncoding("UTF-8");
		httpRequest.set(servletRequest);
		httpResponse.set(servletResponse);

		Request appRequest = null;
		try {
			String sessionToken = ServletSupport.getCookieValue(servletRequest, SESSION_TOKEN_KEY);
			String userId = ServletSupport.getCookieValue(servletRequest, USER_ID_KEY);
			if("".equals(sessionToken)) {
				sessionToken = null;
			}
			if("".equals(userId)) {
				userId = null;
			}
 			//if(accessManager != null) {
				appRequest = accessManager.bindRequest(this);
			//}
			Session session;
			if(sessionToken != null) {
				session = appRequest.resolveSession(sessionToken, userId);
			} else {
				try {
					session = getAccessByToken((HttpServletRequest) servletRequest, appRequest);
				} catch (Exception e) {
					System.out.println(new LogEntry(Level.CRITICAL, "error while obtaining session by token", e));
					session = null;
				}
			}
			if(session == null) {
				System.out.println(new LogEntry(Level.VERBOSE, "no session yet for IP " + ((HttpServletRequest)servletRequest).getHeader("X-Forwarded-For")));
			}
/*
			if (this.syncUserPrefs && appRequest.getTimesEntered() == 0) {
				//pass user preferences here
				ServletSupport.importCookieValues(servletRequest,  appRequest.getUserSettings());
				ServletSupport.exportCookieValues(servletResponse,  appRequest.getUserSettings(), "/", userPrefsMaxAge, Arrays.asList(new String[]{SESSION_TOKEN_KEY}));
			}
*/
			//if a user logged in, the user id must be stored
			if(userId == null)
			{
				User user = appRequest.getUser();
				if(user != null)
				{
					storeSessionDataInCookie(USER_ID_KEY, user.getId(), servletResponse);
				}
			}

			if(loginRequired) {
				User user = appRequest.getUser();
				//String pathInfo;
				//String servletPath =
				String pathInfo = getPath(servletRequest);

				if(user == null && contentNotPublic(pathInfo)) {
					System.out.println(new LogEntry("" + pathInfo + " == " + loginPath));
					if(!(pathInfo.equals(loginPath) || pathInfo.equals("/"))) {
						System.out.println(new LogEntry("user must authenticate first to obtain " + pathInfo));
						ServletSupport.respond((HttpServletResponse)servletResponse, "Session expired ...", 401);
						return;
					}
				}
			}

			//role based access control
	//		checkAccess(servletRequest,  appRequest);

			long start = System.currentTimeMillis();

			//delegate request
			chain.doFilter(servletRequest, servletResponse);

			long timeUsed = System.currentTimeMillis() - start;
			if(timeUsed > 100) {
				System.out.println(new LogEntry(Level.VERBOSE, "Handling request: " + getPath(servletRequest)
						+ " took " + timeUsed + "ms." ));
			}
		}
		catch (Throwable t)//make sure user gets a controlled response
		{
			//is this the actual entry point or is this entry point wrapped?
			if ( appRequest != null &&  appRequest.getTimesEntered() > 1)
			{
				//it's wrapped, so the exception must be thrown at the top entry point
				ServletSupport.rethrow(t);
			}
			handleException(servletRequest, servletResponse, t);
		}
		finally
		{
/*			if(loggingEnabled)
			{
				application.log(new PageVisitLogEntry((HttpServletRequest) servletRequest));
			}
			application.releaseRequest();  */
			if(accessManager != null) {
				accessManager.releaseRequest();
			}
		}
	}

	private String getPath(ServletRequest servletRequest) {
		String pathInfo = ((HttpServletRequest) servletRequest).getPathInfo();
		if(pathInfo == null) {
			pathInfo = ((HttpServletRequest)servletRequest).getServletPath();
		}
		return pathInfo;
	}

	private Session getAccessByToken(HttpServletRequest servletRequest, Request request) {
		AuthorizationBearer authorizationBearer = AuthorizationBearer.getHttpHeader(servletRequest);
		Session session = null;
		if(authorizationBearer != null) {
			Credentials credentials = decodeCredentials(authorizationBearer.getToken());
			session = request.resolveSession(authorizationBearer.getToken(), credentials.getUserId());
			if(session == null) {
				accessManager.createSession(authorizationBearer.getToken(), new Properties());
				session = request.resolveSession(authorizationBearer.getToken(), credentials.getUserId());
				User user = session.login(credentials);
				System.out.println(new LogEntry(Level.TRACE, "session created " + user));
			} else {
				System.out.println(new LogEntry(Level.TRACE, "session resolved " + session.getUser()));
			}
		}
		return session;
	}

	private Credentials decodeCredentials(String encodedCredentials) {
		String decodedCredentials = decodeString(encodedCredentials);
		List<String> credentialsArray = StringSupport.split(decodedCredentials, ":");
		if (credentialsArray.size() < 2) {
			throw new IllegalArgumentException("wrong format of encoded credentials: colon separator not found");
		}
		Credentials credentials = new SimpleCredentials(credentialsArray.get(0), credentialsArray.get(1));
		return credentials;
	}

	protected String decodeString(String encodedString) {
		if(xorKey != null) {
			return EncodingSupport.decodeXor(encodedString, xorKey);
		}
		throw new ConfigurationException("XOR key unavailable");
	}


	private boolean contentNotPublic(String servletPath) {
		return (publicContentRegExp == null || !PatternMatchingSupport.valueMatchesRegularExpression(servletPath, publicContentRegExp));
	}

	private boolean isStaticContent(String servletPath) {
		return staticContentRegExp != null && PatternMatchingSupport.valueMatchesRegularExpression(servletPath, staticContentRegExp);
	}


	/**
	 * Is invoked in case an exception or error occurred in a servlet that was not handled by
	 * the implementating code
	 * <p/>
	 * An attempt is made to redirect the request to an URL defined in the configuration
	 * at section "SERVLET_EXCEPTION_REDIRECTS", key "unhandled_error"
	 * <p/>
	 * Override this method to implement your own error / exception handling
	 *
	 * @param request
	 * @param response
	 * @param cause
	 */
	public void handleException(ServletRequest request, ServletResponse response, Throwable cause) throws IOException, ServletException
	{
		List messageStack = new ArrayList();
		messageStack.add("Request: " + request);
		messageStack.add("Remote address: " + request.getRemoteAddr());
		messageStack.add("Remote host: " + request.getRemoteHost());

		//all servlets are responsible for handling all possible situations
		//so an exception handled here is a critical one
		if(cause instanceof ServletRequestAlreadyRedirectedException)
		{
			return;
		}

 		//print error to screen
		if(this.printUnhandledExceptions) {
			if(!response.isCommitted())	{
				System.out.println(new LogEntry(Level.CRITICAL, "exception handled in http-filter " + filterName, cause));
				ServletSupport.printException(response, "An exception occurred for which no exception page is defined.\n" +
						"Make sure you do so if your application is in a production environment.\n" +
						"(in section [" + exceptionPagesSectionId + "])" +
						"\n\n" + CollectionSupport.format(messageStack, "\n"), cause);
			} else {
				System.out.println(new LogEntry(Level.CRITICAL, "exception handled in http-filter " + filterName + " can not be printed: response already committed", cause));
			}
		}
	}



	/**
	 * Is invoked when the servlet runner shuts down
	 */
	public void destroy() {
	}




}

