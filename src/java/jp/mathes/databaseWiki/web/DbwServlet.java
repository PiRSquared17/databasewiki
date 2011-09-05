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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jp.mathes.databaseWiki.db.Backend;
import jp.mathes.databaseWiki.db.BackendException;
import jp.mathes.databaseWiki.db.Document;
import jp.mathes.databaseWiki.db.DocumentNotFoundException;
import jp.mathes.databaseWiki.db.Field;
import jp.mathes.databaseWiki.db.FieldType;
import jp.mathes.databaseWiki.wiki.Plugin;
import jp.mathes.databaseWiki.wiki.PluginException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import freemarker.cache.ClassTemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModelException;

public class DbwServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	transient private Configuration cfg;
	transient private Backend backend;
	transient private List<Plugin> plugins;

	@Override
	public void init() throws ServletException {
		super.init();
		this.cfg = new Configuration();
		this.cfg.setTemplateLoader(new ClassTemplateLoader(this.getClass(),
			"templates"));
		this.backend = DbwConfiguration.getInstance().getBackend();
		this.plugins = DbwConfiguration.getInstance().getPlugins();
	}

	private void addCommonData(final Map<String, Object> data,
		final HttpServletRequest req, final String db, final String table,
		final String name) {
		data.put("title", String.format("%s &gt; %s &gt; %s", db, table, name));
		data.put("context", req.getContextPath());
		try {
			BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
			TemplateHashModel enumModels = wrapper.getEnumModels();
			TemplateHashModel fieldTypeStatic = (TemplateHashModel) enumModels
				.get("jp.mathes.databaseWiki.db.FieldType");
			TemplateHashModel fieldUsageStatic = (TemplateHashModel) enumModels
				.get("jp.mathes.databaseWiki.db.FieldUsage");
			data.put("FieldType", fieldTypeStatic);
			data.put("FieldUsage", fieldUsageStatic);
		} catch (TemplateModelException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private void redirect(final String baseURL, final HttpServletRequest req,
		final HttpServletResponse resp, final String db, final String table,
		String name, final boolean withParameters) throws IOException {
		name = URLEncoder.encode(name,"UTF-8");
		StringBuilder paramStr = new StringBuilder();
		if (withParameters) {
			for (String paramName : Collections.list((Enumeration<String>) req
				.getParameterNames())) {
				if (paramName.startsWith("_") || paramName.equals("name")) {
					continue;
				}
				for (String value : req.getParameterValues(paramName)) {
					paramStr.append(paramName).append("=").append(value).append("&");
				}
			}
		}
		if (paramStr.length() > 0) {
			resp.sendRedirect(String.format(baseURL + "?%s", req.getContextPath(),
				db, table, name, paramStr.toString()));
		} else {
			resp.sendRedirect(String.format(baseURL, req.getContextPath(), db, table,
				name));
		}
	}

	@SuppressWarnings("rawtypes")
	private void view(final String db, final String table, final String name,
		final String user, final String password, final Configuration cfg,
		final HttpServletResponse resp, final HttpServletRequest req)
		throws IOException, TemplateException, BackendException, PluginException {
		Document document = this.backend.getDocument(user, password, db, table,
			name, false);
		Template template = cfg.getTemplate("view.ftl");
		HashMap<String, Object> data = new HashMap<String, Object>();
		Map<String, Field> allFields = document.getAllFields();
		for (Field field : allFields.values()) {
			if (field.getType() == FieldType.text) {
				for (Plugin plugin : this.plugins) {
					plugin.process(document, field.getName(), user, password,
						this.backend);
				}
			}
		}
		data.put("fields", allFields);
		this.addCommonData(data, req, db, table, name);
		template.process(data, resp.getWriter());
	}

	@SuppressWarnings("unchecked")
	private void edit(final String db, final String table, final String name,
		final String user, final String password, final Configuration cfg,
		final HttpServletResponse resp, final HttpServletRequest req,
		final Backend backend) throws IOException, TemplateException,
		BackendException {
		Map<String, String[]> parameterMap = req.getParameterMap();
		Document document = backend.getDocument(user, password, db, table, name,
			true, parameterMap);
		Template template = cfg.getTemplate("edit.ftl");
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put("fields", document.getAllFields());
		this.addCommonData(data, req, db, table, name);
		template.process(data, resp.getWriter());
	}

	@SuppressWarnings("unchecked")
	private void save(final String db, final String table, final String name,
		final String user, final String password, final HttpServletRequest req)
		throws BackendException {
		Document document = this.backend.getDocument(user, password, db, table,
			name, true);
		for (String fieldName : document.getAllFields().keySet()) {
			if (req.getParameter("fields." + fieldName) != null
				&& req.getParameter("fields." + fieldName).length() > 0) {
				document.getAllFields().get(fieldName)
					.setValue(req.getParameter("fields." + fieldName));
			}
		}
		this.backend.saveDocument(user, password, db, table, name, document);
	}

	private void delete(final String db, final String table, final String name,
		final String user, final String password) throws BackendException {
		this.backend.deleteDocument(user, password, db, table, name);
	}

	private void showNames(final String db, final String table,
		final String user, final String password, final Configuration cfg2,
		final HttpServletResponse resp, final HttpServletRequest req)
		throws IOException, TemplateException, BackendException {
		List<String> names = this.backend.getNames(user, password, db, table);
		Template template = this.cfg.getTemplate("list.ftl");
		HashMap<String, Object> data = new HashMap<String, Object>();
		this.addCommonData(data, req, db, table, "");
		List<String> urls = new LinkedList<String>();
		for (String name : names) {
			urls.add(db + "/" + table + "/" + name);
		}
		data.put("urls", urls);
		template.process(data, resp.getWriter());
	}

	private void showTables(final String db, final String user,
		final String password, final Configuration cfg2,
		final HttpServletResponse resp, final HttpServletRequest req)
		throws TemplateException, IOException, BackendException {
		List<String> tables = this.backend.getTables(user, password, db);
		Template template = this.cfg.getTemplate("list.ftl");
		HashMap<String, Object> data = new HashMap<String, Object>();
		this.addCommonData(data, req, db, "", "");
		List<String> urls = new LinkedList<String>();
		for (String table : tables) {
			urls.add(db + "/" + table);
		}
		data.put("urls", urls);
		template.process(data, resp.getWriter());
	}

	private void executeSQL(final HttpServletRequest req,
		final HttpServletResponse resp, final String user, final String password,
		final String db, final String table, final String name) throws IOException,
		BackendException {
		String statement = req.getParameter("statement") != null ? req
			.getParameter("statement") : "";
		this.backend.executeUpdate(user, password, db, statement);
		this.redirect("%s/%s/%s/%s/view", req, resp, db, table, name, false);
	}

	private void handleAction(final HttpServletRequest req,
		final HttpServletResponse resp, final String user, final String password)
		throws InstantiationException, IllegalAccessException,
		ClassNotFoundException, IOException, TemplateException, BackendException,
		PluginException {

		String action = req.getParameter("_action") != null ? req
			.getParameter("_action") : "";
		String db = req.getParameter("_db") != null ? req.getParameter("_db") : "";
		String table = req.getParameter("_table") != null ? req
			.getParameter("_table") : "";
		String name = req.getParameter("name") != null ? URLDecoder.decode(
			req.getParameter("name"), "UTF-8") : "";

		if ("sql".equals(action)) {
			this.executeSQL(req, resp, user, password, db, table, name);
		}

		if (StringUtils.isEmpty(db)) {
			throw new BackendException(
				"URL must be given as /db/[table[/name]][/action].");
		} else if (StringUtils.isEmpty(table)) {
			this.showTables(db, user, password, this.cfg, resp, req);
		} else if (StringUtils.isEmpty(name)) {
			this.showNames(db, table, user, password, this.cfg, resp, req);
		} else if (StringUtils.isEmpty(action)) {
			this.redirect("%s/%s/%s/%s/view", req, resp, db, table, name, true);
		} else if ("view".equals(action)) {
			try {
				this.view(db, table, name, user, password, this.cfg, resp, req);
			} catch (DocumentNotFoundException e) {
				this.redirect("%s/%s/%s/%s/edit", req, resp, db, table, name, true);
			}
		} else if ("edit".equals(action)) {
			this.edit(db, table, name, user, password, this.cfg, resp, req,
				this.backend);
		} else if ("save".equals(action)) {
			this.save(db, table, name, user, password, req);
			resp.sendRedirect("view");
		} else if ("delete".equals(action)) {
			this.delete(db, table, name, user, password);
			resp.sendRedirect("view");
		}
	}

	private void handleHttp(final HttpServletRequest req,
		final HttpServletResponse resp) throws ServletException, IOException {
		req.setCharacterEncoding("UTF-8");
		resp.setCharacterEncoding("UTF-8");
		String user = null;
		String password = null;
		if (req.getHeader("Authorization") != null) {
			String[] split = req.getHeader("Authorization").split(" ");
			String userpw = "";
			if (split.length > 1) {
				userpw = new String(Base64.decodeBase64(split[1]));
			}
			user = StringUtils.substringBefore(userpw, ":");
			password = StringUtils.substringAfter(userpw, ":");
			resp.setStatus(HttpServletResponse.SC_OK);
			resp.setContentType("application/xhtml+xml; charset=UTF-8");
			try {
				this.handleAction(req, resp, user, password);
			} catch (InstantiationException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Could not instantiate database backend: " + e.getMessage());
			} catch (IllegalAccessException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Illegal Access: " + e.getMessage());
			} catch (ClassNotFoundException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Could not find database backend: " + e.getMessage());
			} catch (TemplateException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Template error: " + e.getMessage());
			} catch (BackendException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Database error: " + e.getMessage());
			} catch (PluginException e) {
				resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
					"Rendering error: " + e.getMessage());
			}
		} else {
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setHeader("WWW-Authenticate", "Basic realm=\"databaseWiki\"");
		}
	}

	@Override
	protected void doGet(final HttpServletRequest req,
		final HttpServletResponse resp) throws ServletException, IOException {
		this.handleHttp(req, resp);
	}

	@Override
	protected void doPost(final HttpServletRequest req,
		final HttpServletResponse resp) throws ServletException, IOException {
		this.handleHttp(req, resp);
	}
}
