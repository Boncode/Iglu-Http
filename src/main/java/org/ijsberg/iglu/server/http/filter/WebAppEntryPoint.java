package org.ijsberg.iglu.server.http.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.ijsberg.iglu.access.*;
import org.ijsberg.iglu.configuration.ConfigurationException;
import org.ijsberg.iglu.http.client.AuthorizationBearer;
import org.ijsberg.iglu.logging.Level;
import org.ijsberg.iglu.logging.LogEntry;
import org.ijsberg.iglu.rest.RestException;
import org.ijsberg.iglu.server.http.servlet.ServletRequestAlreadyRedirectedException;
import org.ijsberg.iglu.util.collection.CollectionSupport;
import org.ijsberg.iglu.util.formatting.PatternMatchingSupport;
import org.ijsberg.iglu.util.http.ServletSupport;
import org.ijsberg.iglu.util.misc.EncodingSupport;
import org.ijsberg.iglu.util.misc.KeyGenerator;
import org.ijsberg.iglu.util.misc.StringSupport;
import org.ijsberg.iglu.util.properties.IgluProperties;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import static org.ijsberg.iglu.http.client.Constants.ATTRIBUTE_SESSION_RESOLVED_BY_TOKEN;
import static org.ijsberg.iglu.http.client.Constants.HEADER_X_CSRF_TOKEN;


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
public class WebAppEntryPoint implements Filter, EntryPoint {

	private static final String SESSION_TOKEN_KEY = "IGLU_SESSION_TOKEN";

	private String xorKey;

	private AccessManager accessManager;

	protected String filterName;

	private int userPrefsMaxAge = -1;

	//debug mode
	private final boolean printUnhandledExceptions = false;

	private ThreadLocal httpRequest = new ThreadLocal();
	private ThreadLocal httpResponse = new ThreadLocal();

	private boolean loginRequired = false;
	private String loginPath;

	private String publicContentRegExp;
	private String staticContentRegExp;

//	private boolean passSessionIdSecure = false; // removed, always pass securely

	private final IgluProperties additionalHeaders = new IgluProperties();
	private final IgluProperties additionalHeadersNoLogin = new IgluProperties();
	private final IgluProperties additionalStaticContentHeaders = new IgluProperties();


	//todo this might be nice to do a controlled response redirect for pages that don't exist etc
//	private static class ExceptionHandlingSettings {
//		public String redirectPage;
//		public int loglevel;
//
//
//		public ExceptionHandlingSettings(String redirectPage, int loglevel) {
//			this.redirectPage = redirectPage;
//			this.loglevel = loglevel;
//		}
//	}


    public WebAppEntryPoint()
	{
		super();
	}

	public void setAccessManager(AccessManager accessManager) {
		this.accessManager = accessManager;
	}

	public void setXorKey(String xorKey) {
		this.xorKey = xorKey;
	}

	/**
	 * Notification of a session registration event.
	 *
	 * @param session
	 */
	public void onSessionUpdate(Request currentRequest, Session session) {
		HttpServletResponse response = (HttpServletResponse)httpResponse.get();
		ServletSupport.setCookieValue(response, SESSION_TOKEN_KEY, session.getToken());
	}

	/**
	 * Notification of a session destruction.
	 *
	 * @param session
	 */
	public void onSessionDestruction(Request currentRequest, Session session) {
		HttpServletResponse response = (HttpServletResponse)httpResponse.get();
		ServletSupport.setCookieValue(response, SESSION_TOKEN_KEY, null);
	}

    public void exportUserSettings(Request currentRequest, Properties properties) {
		HttpServletResponse response = (HttpServletResponse)httpResponse.get();
		ServletSupport.exportCookieValues(response, properties, "/", userPrefsMaxAge);
	}

    public void importUserSettings(Request currentRequest, Properties properties) {
		ServletRequest request = (ServletRequest)httpRequest.get();
		ServletSupport.importCookieValues(request, properties);
	}

	//TODO document init params
	public final void init(FilterConfig conf) {
		filterName = conf.getFilterName();

		String loginRequiredStr = conf.getInitParameter("login_required");

		loginRequired = (loginRequiredStr != null ? Boolean.valueOf(loginRequiredStr) : true);
		loginPath = conf.getInitParameter("login_path");

		//pass_session_id_secure
//		String passSessionIdSecureStr = conf.getInitParameter("pass_session_id_secure");
//		passSessionIdSecure = (passSessionIdSecureStr != null ? Boolean.valueOf(passSessionIdSecureStr) : false);

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
				additionalHeaders.setProperty(initParamName.substring("header.".length()), conf.getInitParameter(initParamName));
			}
			if(initParamName.startsWith("login_page_header.")) {
				additionalHeadersNoLogin.setProperty(initParamName.substring("login_page_header.".length()), conf.getInitParameter(initParamName));
			}
			if(initParamName.startsWith("static_content_header.")) {
				additionalStaticContentHeaders.setProperty(initParamName.substring("static_content_header.".length()), conf.getInitParameter(initParamName));
			}
		}
	}

	/**
	 * Enable CORS requests for development (frontend is running on localhost:3000 in React)
	 * So some extra headers are provided through this method to allow for requests from this port
	 * @param corsProperties
	 */
	public void enableCORS(IgluProperties corsProperties) {
		for(String headerName : corsProperties.getRootKeys()) {
			additionalHeaders.setProperty(headerName, corsProperties.getProperty(headerName));
		}
	}

	/**
	 * Must handle all incoming http requests.
	 * Contains hooks for request and session management.
	 * After filtering, request is delegated to the corresponding servlet for the path, so /auth/* for example delegates
	 * to the IgluRestServlet that has the AuthenticationAgent bound to it.
	 * @param servletRequest
	 * @param servletResponse
	 * @throws IOException
	 */
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws ServletException, IOException {
		//todo this could be done with a new jetty filter instead of in here
		if(((HttpServletRequest)servletRequest).getMethod().equals("OPTIONS")) {
			setResponseHeaders(servletRequest, servletResponse, null);
			((HttpServletResponse)servletResponse).setStatus(HttpServletResponse.SC_OK);
			return;
		}

		String pathInfo = getPath(servletRequest);

		servletRequest.setCharacterEncoding("UTF-8");
		httpRequest.set(servletRequest);
		httpResponse.set(servletResponse);

		Request appRequest = null;
		try {
			String sessionToken = getCookieValueNullIfEmpty(servletRequest, SESSION_TOKEN_KEY);

			appRequest = accessManager.bindRequest(this);
			try {
				appRequest.setAttribute("IP-Address", getClientIpAddress((HttpServletRequest) servletRequest));
			} catch (Throwable t) {
				System.out.println(new LogEntry("retrieving IP-adddress failed", t));
			}

			Session session;
			if(sessionToken != null) {
				session = appRequest.resolveSession(sessionToken, null);
			} else {
				try {
					session = getAccessByToken((HttpServletRequest) servletRequest, appRequest);
					//TODO CSRF should not be enforced
				} catch (Exception e) {
					System.out.println(new LogEntry(Level.CRITICAL, "error while obtaining session by token", e));
					session = null;
				}
			}
			if(session == null) {
				System.out.println(new LogEntry(Level.VERBOSE, "no session yet for IP " + ((HttpServletRequest)servletRequest).getHeader("X-Forwarded-For")));
			}

			//set response headers
			setResponseHeaders(servletRequest, servletResponse, session);

			if(loginRequired) {
				User user = appRequest.getUser();

				if(user == null && contentNotPublic(pathInfo)) {
					System.out.println(new LogEntry("" + pathInfo + " == " + loginPath));
					if(!(pathInfo.equals(loginPath) || pathInfo.equals("/"))) {
						System.out.println(new LogEntry("user must authenticate first to obtain " + pathInfo));
						ServletSupport.respond((HttpServletResponse)servletResponse, "Session expired ...", 401);
						return;
					}
				}
			}

			long start = System.currentTimeMillis();

			//delegate request
			chain.doFilter(servletRequest, servletResponse);

			passCsrfToken(servletResponse, pathInfo);

			long timeUsed = System.currentTimeMillis() - start;
			if(timeUsed > 100) {
				System.out.println(new LogEntry(Level.VERBOSE, "Handling request: " + getPath(servletRequest)
						+ " took " + timeUsed + "ms. (> 100)" ));
			}
		}
		catch (Throwable t) { //make sure user gets a controlled response
			//is this the actual entry point or is this entry point wrapped?
			if ( appRequest != null &&  appRequest.getTimesEntered() > 1) {
				//it's wrapped, so the exception must be thrown at the top entry point
				ServletSupport.rethrow(t);
			}
			handleException(servletRequest, (HttpServletResponse)servletResponse, t);
		} finally {
			if(accessManager != null) {
				accessManager.releaseRequest();
			}
		}
	}

	private static String getCookieValueNullIfEmpty(ServletRequest request, String cookieName) {
		String cookieValue = ServletSupport.getCookieValue(request, cookieName);
		if (cookieValue != null && cookieValue.isEmpty()) {
			cookieValue = null;
		}
		return cookieValue;
	}

	/**
	 * Retrieves the client IP address for a given HttpServletRequest.
	 * First tries the X-Forwarded-For header. If that holds no value it falls back on the remote address. If the
	 * X-Forwarded-For header contains multiple IPs (caused by load-balancers and/or proxies), we take the left-most
	 * value, which is usually the client IP.
	 * @param servletRequest
	 * @return the client IP address as a string
	 */
	private static String getClientIpAddress(HttpServletRequest servletRequest) {
		String xForwardedFor = servletRequest.getHeader("X-Forwarded-For");
		if(xForwardedFor == null) {
			return servletRequest.getRemoteAddr();
		}

		// could be multiple forward/reverse proxies, client ip is first in the list
		if(xForwardedFor.contains(",")) {
			return xForwardedFor.split(",")[0];
		}
		return xForwardedFor;
	}

	/**
	 * Adds a token to AjaxRequestManager to prevent Cross Script Request Forgery.
	 * The IgluRestServlet performs a check if the CSRF-token is valid.
	 * We assume that the AjaxRequestManager client-side and the IgluRestServlet server-side are used for client-server interaction.
	 *
	 * @param servletResponse
	 * @param pathInfo
	 * @throws IOException
	 */
	private void passCsrfToken(ServletResponse servletResponse, String pathInfo) throws IOException {
		Request request = accessManager.getCurrentRequest();
		String csrfToken = null;
		if (request.hasSession() && !isStaticContent(pathInfo)) {
			Session session = request.getSession(false);
			csrfToken = (String)session.getAttribute(HEADER_X_CSRF_TOKEN);
			if(csrfToken == null) {
				csrfToken = KeyGenerator.generateKey(60);
				session.setAttribute(HEADER_X_CSRF_TOKEN, csrfToken);
			}
			((HttpServletResponse)servletResponse).setHeader(HEADER_X_CSRF_TOKEN, csrfToken);
		}
	}

	private void setResponseHeaders(ServletRequest servletRequest, ServletResponse servletResponse, Session session) {
		for(String header : additionalHeaders.toOrderedMap().keySet()) {
			String additionalHeaderValue = additionalHeaders.getProperty(header);
			if(isStaticContent(getPath(servletRequest))) {
				//System.out.println(getPath(servletRequest) + " MATCHES " + staticContentRegExp);
				if(additionalStaticContentHeaders.containsKey(header)) {
					additionalHeaderValue = additionalStaticContentHeaders.getProperty(header);
				}
			}
			//check user logged in
			if(session != null && session.getUser() == null) {
				if(additionalHeadersNoLogin.containsKey(header)) {
					additionalHeaderValue = additionalHeadersNoLogin.getProperty(header);
				}
			}
			((HttpServletResponse)servletResponse).addHeader(header, additionalHeaderValue);
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
				if (user != null) {
					System.out.println(new LogEntry(Level.TRACE, "session created for user: " + user));
					session.setAttribute(ATTRIBUTE_SESSION_RESOLVED_BY_TOKEN, true);
				} else {
					session = null;
					System.out.println(new LogEntry(Level.CRITICAL, "session not created because login failed for token containing user ID " + credentials.getUserId()));
				}
			} else {
				System.out.println(new LogEntry(Level.TRACE, "session resolved " + session.getUser()));
				session.setAttribute(ATTRIBUTE_SESSION_RESOLVED_BY_TOKEN, true);
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
		return new SimpleCredentials(credentialsArray.get(0), credentialsArray.get(1));
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
		return (staticContentRegExp != null && PatternMatchingSupport.valueMatchesRegularExpression(servletPath, staticContentRegExp));
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
	public void handleException(ServletRequest request, HttpServletResponse response, Throwable cause) throws IOException, ServletException
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

		if(cause instanceof RestException)
		{
			if(!response.isCommitted())	{
				response.getOutputStream().println(((RestException)cause).getMessage());
				response.setStatus(((RestException)cause).getHttpStatusCode());
			}
		}

		System.out.println(new LogEntry(Level.CRITICAL, "exception handled in http-filter " + filterName, cause));

		//print error to screen (for debugging purposes)
		if(this.printUnhandledExceptions) {
			if(!response.isCommitted())	{
				ServletSupport.printException(response, "An exception occurred for which no exception page is defined.\n" +
						"Make sure you do so if your application is in a production environment.\n" +
						"\n\n" + CollectionSupport.format(messageStack, "\n"), cause);
				response.setStatus(500);
			}
		}
	}

	/**
	 * Is invoked when the servlet runner shuts down
	 */
	public void destroy() {
	}
}

