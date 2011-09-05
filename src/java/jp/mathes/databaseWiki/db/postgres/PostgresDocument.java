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
package jp.mathes.databaseWiki.db.postgres;

import java.util.LinkedHashMap;
import java.util.Map;

import jp.mathes.databaseWiki.db.Document;
import jp.mathes.databaseWiki.db.Field;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class PostgresDocument implements Document {

	@SuppressWarnings({ "rawtypes" })
	private Map<String, Field> fields = new LinkedHashMap<String, Field>();

	private String database;
	private String table;
	private String name;

	@SuppressWarnings({ "rawtypes" })
	@Override
	public Map<String, Field> getAllFields() {
		return this.fields;
	}

	@SuppressWarnings({ "rawtypes" })
	public void addField(final String name, final Field field) {
		this.fields.put(name, field);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	@Override
	public String getDatabase() {
		return this.database;
	}

	@Override
	public String getTable() {
		return this.table;
	}

	@Override
	public String getName() {
		return this.name;
	}

	protected void setDatabase(final String database) {
		this.database = database;
	}

	protected void setTable(final String table) {
		this.table = table;
	}

	protected void setName(final String name) {
		this.name = name;
	}
}
