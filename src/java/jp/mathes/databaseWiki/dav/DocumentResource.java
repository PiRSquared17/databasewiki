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
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jp.mathes.databaseWiki.db.BackendException;
import jp.mathes.databaseWiki.db.Document;
import jp.mathes.databaseWiki.web.DbwConfiguration;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import freemarker.cache.ClassTemplateLoader;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateHashModel;

public class DocumentResource implements PropFindableResource, GetableResource,
	DeletableResource {

	private String dbName;
	private String tableName;
	private String name;
	private String content;
	private String user;
	private String password;

	public DocumentResource(final String dbName, final String tableName,
		final String name) {
		this.dbName = dbName;
		this.tableName = tableName;
		this.name = name;
	}

	public DocumentResource(final String dbName, final String tableName,
		final String name, final String user, final String password) {
		this.dbName = dbName;
		this.tableName = tableName;
		this.name = name;
		this.user = user;
		this.password = password;
	}

	@Override
	public Object authenticate(final String user, final String password) {
		this.user = user;
		this.password = password;
		return user;
	}

	@Override
	public boolean authorise(final Request request, final Method method,
		final Auth auth) {
		return true;
	}

	@Override
	public String checkRedirect(final Request arg0) {
		return null;
	}

	@Override
	public Date getModifiedDate() {
		return new Date();
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getRealm() {
		return "databaseWiki";
	}

	@Override
	public String getUniqueId() {
		return this.dbName + "/" + this.tableName + "/" + this.name;
	}

	@Override
	public Long getContentLength() {
		if (this.content == null) {
			this.content = this.getContent();
		}
		return (long) this.content.getBytes().length;
	}

	@Override
	public String getContentType(final String arg0) {
		return "text/plain";
	}

	@Override
	public Long getMaxAgeSeconds(final Auth auth) {
		return null;
	}

	@Override
	public void sendContent(final OutputStream out, final Range range,
		final Map<String, String> params, final String contentType)
		throws IOException, NotAuthorizedException, BadRequestException {
		if (range == null) {
			out.write(this.content.getBytes());
		} else {
			out.write(this.content.substring((int) range.getStart(),
				(int) range.getFinish()).getBytes());
		}
	}

	@Override
	public Date getCreateDate() {
		return null;
	}

	private String getContent() {
		Configuration cfg = new Configuration();
		cfg
			.setTemplateLoader(new ClassTemplateLoader(this.getClass(), "templates"));
		try {
			Document document = DbwConfiguration
				.getInstance()
				.getBackend()
				.getDocument(this.user, this.password, this.dbName, this.tableName,
					this.name, true, null);
			Template template = cfg.getTemplate("dav.ftl");
			HashMap<String, Object> data = new HashMap<String, Object>();
			BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
			TemplateHashModel enumModels = wrapper.getEnumModels();
			TemplateHashModel fieldTypeStatic = (TemplateHashModel) enumModels
				.get("jp.mathes.databaseWiki.db.FieldType");
			TemplateHashModel fieldUsageStatic = (TemplateHashModel) enumModels
				.get("jp.mathes.databaseWiki.db.FieldUsage");
			data.put("FieldType", fieldTypeStatic);
			data.put("FieldUsage", fieldUsageStatic);
			data.put("fields", document.getAllFields());
			StringWriter sw = new StringWriter();
			template.process(data, sw);
			return sw.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}

	@Override
	public void delete() throws NotAuthorizedException, ConflictException,
		BadRequestException {
		try {
			DbwConfiguration
				.getInstance()
				.getBackend()
				.deleteDocument(this.user, this.password, this.dbName, this.tableName,
					this.name);
		} catch (BackendException e) {
			throw new BadRequestException(this, e.getMessage());
		}
	}
}
