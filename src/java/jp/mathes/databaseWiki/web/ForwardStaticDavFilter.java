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
package jp.mathes.databaseWiki.web;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class ForwardStaticDavFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(final ServletRequest req, final ServletResponse resp,
		final FilterChain chain) throws IOException, ServletException {
		req.setCharacterEncoding("UTF-8");
		HttpServletRequest httpRequest = (HttpServletRequest) req;
		String incomingUrl = httpRequest.getRequestURI().substring(
			httpRequest.getContextPath().length());
		if (incomingUrl.startsWith("/_/dav/")) {
			httpRequest.getRequestDispatcher(incomingUrl).forward(req, resp);
		} else if (incomingUrl.startsWith("/_/")) {
			httpRequest.getRequestDispatcher(incomingUrl.substring(2)).forward(req,
				resp);
		} else {
			chain.doFilter(req, resp);
		}
	}

	@Override
	public void init(final FilterConfig arg0) throws ServletException {
	}
}
