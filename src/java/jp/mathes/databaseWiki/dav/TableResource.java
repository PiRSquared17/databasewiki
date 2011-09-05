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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import jp.mathes.databaseWiki.db.BackendException;
import jp.mathes.databaseWiki.db.Document;
import jp.mathes.databaseWiki.db.DocumentNotFoundException;
import jp.mathes.databaseWiki.web.DbwConfiguration;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

public class TableResource implements PropFindableResource, CollectionResource,
	PutableResource {

	private String dbName;
	private String name;
	private String user;
	private String password;

	public TableResource(final String dbName, final String name) {
		this.dbName = dbName;
		this.name = name;
	}

	public TableResource(final String dbName, final String name,
		final String user, final String password) {
		this.dbName = dbName;
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
		return this.dbName + "/" + this.name;
	}

	@Override
	public Resource child(final String name) {
		return new DocumentResource(this.dbName, this.name, name, this.user,
			this.password);
	}

	@Override
	public List<? extends Resource> getChildren() {
		try {
			List<String> documentNames = DbwConfiguration.getInstance().getBackend()
				.getNames(this.user, this.password, this.dbName, this.name);
			List<DocumentResource> documents = new LinkedList<DocumentResource>();
			for (String documentName : documentNames) {
				documents.add(new DocumentResource(this.dbName, this.name,
					documentName, this.user, this.password));
			}
			return documents;
		} catch (BackendException e) {
			DbwConfiguration.getInstance().davLog(
				"BackendException in getChildren()", e);
		}
		return null;
	}

	@Override
	public Date getCreateDate() {
		return null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Resource createNew(String newName, InputStream inputStream,
		Long length, String contentType) throws IOException, ConflictException,
		NotAuthorizedException, BadRequestException {
		try {
			Document doc = DbwConfiguration
				.getInstance()
				.getBackend()
				.getDocument(this.user, this.password, this.dbName, this.name, name,
					true);
			BufferedReader reader = new BufferedReader(new InputStreamReader(
				inputStream));
			String line = reader.readLine();
			while (line != null) {
				if (StringUtils.isEmpty(line) || StringUtils.isWhitespace(line)) {
					// everything after this line is field content
					StringBuffer content = new StringBuffer();
					while ((line = reader.readLine()) != null) {
						if (content.length() > 0) {
							content.append("\n");
						}
						content.append(line);
					}
					if (doc.getAllFields().containsKey("content")) {
						doc.getAllFields().get("content").setValue(content.toString());
					} else {
						DbwConfiguration.getInstance().davLog(
							"Parse error, could not set field 'content'.");
						throw new BadRequestException(this,
							"Parse error, could not set field 'content'.");
					}
				} else if (line.startsWith("\t")) {
					// if the line starts with a tab it has to be the continuation of
					// another line (which is handled in the else path) or is a mistake
					DbwConfiguration.getInstance().davLog(
						"Line starts with tab although it is not a continuation.");
					throw new BadRequestException(this,
						"Line starts with tab although it is not a continuation.");
				} else {
					// this is a regular field, potentially with continuation
					String[] split = StringUtils.split(line, ":", 2);
					String fieldName = split[0].trim();
					StringBuffer fieldValue = new StringBuffer(split[1].trim());
					String nextLine = reader.readLine();
					while (nextLine != null && nextLine.startsWith("\t")) {
						fieldValue.append("\n").append(StringUtils.substring(nextLine, 1));
					}
					line = nextLine;
					if (doc.getAllFields().containsKey(fieldName)) {
						doc.getAllFields().get(fieldName).setValue(fieldValue.toString());
					} else {
						DbwConfiguration.getInstance().davLog(
							String
								.format("Parse error, could not set field '%s'.", fieldName));
						throw new BadRequestException(this, String.format(
							"Parse error, could not set field '%s'.", fieldName));
					}
				}
			}
			DbwConfiguration
				.getInstance()
				.getBackend()
				.saveDocument(this.user, this.password, this.dbName, this.name, name,
					doc);
		} catch (DocumentNotFoundException e) {
			DbwConfiguration.getInstance().davLog(
				"Impossible DocumentNotFoundException in createNew()", e);
			throw new BadRequestException(this,
				"This cannot happen with allowEmpty=true (getDocument).");
		} catch (BackendException e) {
			DbwConfiguration.getInstance().davLog("BackendException in createNew()",
				e);
			throw new BadRequestException(this, e.getMessage());
		}
		return null;
	}
}
