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

import java.util.LinkedList;
import java.util.List;

import jp.mathes.databaseWiki.db.Field;
import jp.mathes.databaseWiki.db.FieldType;
import jp.mathes.databaseWiki.db.FieldUsage;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class PostgresField<T> implements Field<T> {

	private FieldType type;
	private FieldUsage usage;
	private T value;
	private String name;
	private List<T> allowedValues;

	public PostgresField() {
		super();
	}

	public PostgresField(final FieldType type, final FieldUsage usage,
		final String name, final T value) {
		this.type = type;
		this.usage = usage;
		this.name = name;
		this.value = value;
	}

	@Override
	public FieldType getType() {
		return this.type;
	}

	public void setType(final FieldType type) {
		this.type = type;
	}

	@Override
	public FieldUsage getUsage() {
		return this.usage;
	}

	public void setUsage(final FieldUsage usage) {
		this.usage = usage;
	}

	@Override
	public T getValue() {
		return this.value;
	}

	@Override
	public void setValue(final T value) {
		this.value = value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public List<T> getAllowedValues() {
		if (this.allowedValues == null) {
			this.allowedValues = new LinkedList<T>();
		}
		return this.allowedValues;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}