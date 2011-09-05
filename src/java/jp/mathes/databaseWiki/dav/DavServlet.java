/*
   Copyright 2011 Bastian Mathes

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package jp.mathes.databaseWiki.dav;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.bradmcevoy.http.ApplicationConfig;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.ServletHttpManager;
import com.bradmcevoy.http.ServletRequest;
import com.bradmcevoy.http.ServletResponse;

public class DavServlet implements Servlet, ResourceFactory {

	protected ServletHttpManager httpManager;
	private ServletConfig config;
	private String cutoffPath;

	@Override
	public void init(final ServletConfig config) throws ServletException {
		try {
			this.config = config;
			this.cutoffPath = config.getInitParameter("cutoff.path");
			this.httpManager = new ServletHttpManager(this);
			this.httpManager.init(new ApplicationConfig(config), this.httpManager);
		} catch (Throwable ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	public void destroy() {
		if (this.httpManager != null) {
			this.httpManager.destroy(this.httpManager);
		}
	}

	@Override
	public void service(final javax.servlet.ServletRequest servletRequest,
		final javax.servlet.ServletResponse servletResponse)
		throws ServletException, IOException {
		HttpServletRequest req = (HttpServletRequest) servletRequest;
		HttpServletResponse resp = (HttpServletResponse) servletResponse;
		if (req.getHeader("Authorization") != null) {
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType("application/xhtml+xml");
			try {
				Request request = new ServletRequest(req);
				Response response = new ServletResponse(resp);
				this.httpManager.process(request, response);
			} finally {
				servletResponse.getOutputStream().flush();
				servletResponse.flushBuffer();
			}
		} else {
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setHeader("WWW-Authenticate", "Basic realm=\"databaseWiki\"");
		}
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.config;
	}

	@Override
	public String getServletInfo() {
		return "DavServlet";
	}

	@Override
	public Resource getResource(final String host, final String path) {
		String realPath = path.replace(this.cutoffPath, "");
		realPath = StringUtils.strip(realPath, "/");
		String[] realPathSplit = StringUtils.split(realPath, "/");
		if (realPathSplit.length == 1) {
			return new DbResource(realPathSplit[0]);
		} else if (realPathSplit.length == 2) {
			return new TableResource(realPathSplit[0], realPathSplit[1]);
		} else if (realPathSplit.length == 3) {
			return new DocumentResource(realPathSplit[0], realPathSplit[1],
				realPathSplit[2]);
		} else {
			throw new RuntimeException(String.format("Invalid path '%s'", path));
		}
	}
}