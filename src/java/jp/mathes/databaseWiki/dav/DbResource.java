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

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import jp.mathes.databaseWiki.db.BackendException;
import jp.mathes.databaseWiki.web.DbwConfiguration;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;

public class DbResource implements CollectionResource, PropFindableResource {

	private String name;
	private String password;
	private String user;

	public DbResource(final String name) {
		this.name = name;
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
		return this.name;
	}

	@Override
	public Resource child(final String name) {
		return new TableResource(this.name, name, this.user, this.password);
	}

	@Override
	public List<? extends Resource> getChildren() {
		try {
			List<String> tableNames = DbwConfiguration.getInstance().getBackend()
				.getTables(this.user, this.password, this.name);
			List<TableResource> tables = new LinkedList<TableResource>();
			for (String tableName : tableNames) {
				tables.add(new TableResource(this.name, tableName, this.user,
					this.password));
			}
			return tables;
		} catch (BackendException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public Date getCreateDate() {
		return null;
	}
}
