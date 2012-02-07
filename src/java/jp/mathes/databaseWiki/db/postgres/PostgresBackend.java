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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jp.mathes.databaseWiki.db.Backend;
import jp.mathes.databaseWiki.db.BackendException;
import jp.mathes.databaseWiki.db.Document;
import jp.mathes.databaseWiki.db.DocumentNotFoundException;
import jp.mathes.databaseWiki.db.Field;
import jp.mathes.databaseWiki.db.FieldType;
import jp.mathes.databaseWiki.db.FieldUsage;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class PostgresBackend implements Backend {

	public static class PostgresRow implements Row {

		List<Object> fields;
		Map<String, Object> fieldsByName;

		public PostgresRow() {
			this.fields = new LinkedList<Object>();
			this.fieldsByName = new HashMap<String, Object>();
		}

		@Override
		public List<Object> getFields() {
			return this.fields;
		}

		@Override
		public Map<String, Object> getFieldsByName() {
			return this.fieldsByName;
		}
	}

	private File logFile;
	private String host;
	private String port;

	public PostgresBackend() {
		super();
		InputStream resourceAsStream = null;
		try {
			Properties props = new Properties();
			resourceAsStream = this.getClass().getResourceAsStream("/dbw.properties");
			props.load(resourceAsStream);
			String logFileName = props.getProperty("dbw.db.backend.postgres.logfile");
			this.host = props.getProperty("dbw.db.backend.postgres.host");
			this.port = props.getProperty("dbw.db.backend.postgres.port");
			this.logFile = new File(logFileName);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(resourceAsStream);
		}
	}

	private synchronized void logString(final String message, final String user)
		throws BackendException {
		FileWriter fw = null;
		try {
			fw = new FileWriter(this.logFile, true);
			IOUtils
				.write(
					String.format("%s: %s: %s\n", new Date().toString(), user, message),
					fw);
		} catch (IOException e) {
			throw new BackendException(String.format("Cannot write to log file %s.",
				this.logFile.getAbsoluteFile()));
		} finally {
			try {
				if (fw != null) {
					fw.close();
				}
			} catch (IOException e) {
				throw new BackendException(String.format("Cannot close log file %s.",
					this.logFile.getAbsoluteFile()));
			}
		}
	}

	private Connection connectToDB(final String user, final String password,
		final String db) throws SQLException, ClassNotFoundException {
		Class.forName("org.postgresql.Driver");
		return DriverManager.getConnection(
			String.format("jdbc:postgresql://%s:%s/%s?charSet=UTF-8", this.host, this.port,
				this.getPlainDatabaseName(db)), user, password);
	}

	private String getSchemaName(final String table, final String database) {
		// takes an explicit schema from schema.table or database.schema or uses
		// "public" as default. Does not analyze the users search_path
		if (table.contains(".")) {
			return table.split("[.]")[0];
		} else if (database.contains(".")) {
			return database.split("[.]")[1];
		} else {
			return "public";
		}
	}

	private String getPlainTableName(final String table) {
		if (table.contains(".")) {
			return table.split("[.]")[1];
		} else {
			return table;
		}
	}

	private String getPlainDatabaseName(final String database) {
		if (database.contains(".")) {
			return database.split("[.]")[0];
		} else {
			return database;
		}
	}

	private int getNumRows(final ResultSet rs) throws SQLException {
		if (!rs.last()) {
			return 0;
		}
		int result = rs.getRow();
		rs.beforeFirst();
		return result;
	}

	private String getNameField(final Connection conn, final String table,
		final String db) throws BackendException {
		Statement st = null;
		ResultSet rs = null;
		String result = null;

		try {
			String schema = this.getSchemaName(table, db);
			String plainTable = this.getPlainTableName(table);
			st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);

			StringBuilder sb = new StringBuilder("");
			sb.append("select k.column_name ");
			sb.append(" from information_schema.table_constraints c ");
			sb.append("  inner join information_schema.key_column_usage k ");
			sb.append("   on c.constraint_catalog = k.constraint_catalog and ");
			sb.append("      c.constraint_schema = k.constraint_schema and ");
			sb.append("      c.constraint_name = k.constraint_name ");
			sb.append(" where c.constraint_type='PRIMARY KEY' and ");
			sb.append("       c.table_name='%s' and ");
			sb.append("       c.table_schema='%s'");
			String queryString = String.format(sb.toString().replaceAll("[ ]+", " "),
				plainTable, schema);

			this.logString(queryString, "?");
			rs = st.executeQuery(queryString);
			if (this.getNumRows(rs) != 1) {
				throw new BackendException(
					String
						.format(
							"Table %s.%s has no or a multi column primary key which is not supported.",
							this.getSchemaName(table, db), this.getPlainTableName(table)));
			}
			rs.next();
			result = rs.getString(1);
		} catch (SQLException e) {
			throw new BackendException(e);
		} finally {
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(st);
		}
		return result;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private PostgresDocument createEmptyDocument(final Connection conn,
		final String table, final String name, final String db)
		throws BackendException {
		Statement st = null;
		Statement st2 = null;
		ResultSet rs = null;
		ResultSet rs2 = null;
		PostgresDocument doc = new PostgresDocument();
		doc.setTable(this.getSchemaName(table, db) + "."
			+ this.getPlainTableName(table));
		doc.setDatabase(db);
		doc.setName(name);
		try {
			String schema = this.getSchemaName(table, db);
			String plainTable = this.getPlainTableName(table);
			st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);

			StringBuilder sb = new StringBuilder("");
			sb.append("select c.column_name, c.column_default, c.data_type, ccu.table_name, ccu.column_name");
			sb.append("  from information_schema.columns c");
			sb.append("  left join information_schema.key_column_usage kcu");
			sb.append("    on kcu.table_schema = c.table_schema and kcu.table_name = c.table_name");
			sb.append("     and kcu.column_name = c.column_name");
			sb.append("  left join information_schema.table_constraints tc");
			sb.append("    on tc.constraint_type='FOREIGN KEY' and tc.table_schema = c.table_schema");
			sb.append("     and tc.table_name = c.table_name and tc.constraint_name = kcu.constraint_name");
			sb.append("  left join information_schema.constraint_column_usage ccu");
			sb.append("    on ccu.constraint_schema = tc.constraint_schema and ccu.constraint_name = tc.constraint_name");
			sb.append("  where c.table_schema='%s' and c.table_name='%s'");
			sb.append("  order by c.ordinal_position");
			String queryString = String.format(sb.toString().replaceAll("[ ]+", " "),
				schema, plainTable);

			this.logString(queryString, "?");
			rs = st.executeQuery(queryString);
			if (this.getNumRows(rs) == 0) {
				throw new BackendException(String.format(
					"Table %s.%s has no columns which is not supported.",
					this.getSchemaName(table, db), this.getPlainTableName(table)));
			}

			String nameField = this.getNameField(conn, table, db);
			while (rs.next()) {
				String ctype = rs.getString(3);
				String cname = rs.getString(1);
				PostgresField field = null;
				if ("character varying".equals(ctype)) {
					field = new PostgresField<String>();
					field.setType(FieldType.string);
					field.setValue(rs.getString(2));
				} else if ("text".equals(ctype)) {
					field = new PostgresField<String>();
					field.setType(FieldType.text);
					field.setValue(rs.getString(2));
				} else if ("integer".equals(ctype) || "bigint".equals(ctype)
					|| "smallint".equals(ctype) || "real".equals(ctype)) {
					field = new PostgresField<Integer>();
					field.setType(FieldType.dec);
					field.setValue(rs.getInt(2));
				} else if ("numeric".equals(ctype)) {
					field = new PostgresField<Double>();
					field.setType(FieldType.num);
					field.setValue(rs.getDouble(2));
				} else if ("date".equals(ctype)) {
					field = new PostgresField<Date>();
					field.setType(FieldType.date);
					field.setValue(rs.getDate(2));
				}
				if (field != null) {
					field.setName(cname);
					field.setUsage(FieldUsage.normal);
					if (nameField.equals(cname)) {
						field.setValue(name);
					} else if ("version".equals(cname)) {
						field.setUsage(FieldUsage.hidden);
					}

					String foreignTable = rs.getString(4);
					String foreignColumn = rs.getString(5);
					if (!StringUtils.isEmpty(foreignTable)
						&& !StringUtils.isEmpty(foreignColumn)) {
						st2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
							ResultSet.CONCUR_READ_ONLY);
						field.setUsage(FieldUsage.fixed);
						StringBuilder sb2 = new StringBuilder();
						sb2.append("select distinct \"%s\" from \"%s\" order by \"%s\"");
						String queryString2 = String.format(
							sb2.toString().replaceAll("[ ]+", " "), foreignColumn,
							foreignTable, foreignColumn);
						this.logString(queryString2, "?");
						rs2 = st2.executeQuery(queryString2);
						while (rs2.next()) {
							field.getAllowedValues().add(rs2.getObject(1));
						}
					}
					doc.addField(cname, field);
				}
			}
		} catch (SQLException e) {
			throw new BackendException(e);
		} finally {
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(rs2);
			DbUtils.closeQuietly(st);
			DbUtils.closeQuietly(st2);
		}
		return doc;
	}

	@SuppressWarnings({ "rawtypes" })
	private String getUpdateStatement(final Document document, final String name,
		final String table, final String nameField, final String db) {
		StringBuilder sb = new StringBuilder(String.format(
			"update \"%s\".\"%s\" set ", this.getSchemaName(table, db),
			this.getPlainTableName(table)));
		boolean isFirst = true;
		for (String fieldName : document.getAllFields().keySet()) {
			Field field = document.getAllFields().get(fieldName);
			if (field.getValue() == null) {
				continue;
			}
			if (isFirst) {
				isFirst = false;
			} else {
				sb.append(",");
			}
			sb.append("\"").append(field.getName()).append("\" = '");
			sb.append(
				field.getValue().toString().replace("'", "''"))
				.append("'");
		}
		sb.append(String.format(" where \"%s\" = '%s'", nameField, name));
		return sb.toString();
	}

	@SuppressWarnings("rawtypes")
	private String getInsertStatement(final Document document, final String name,
		final String table, final String nameField, final String db) {
		StringBuilder sb = new StringBuilder(String.format(
			"insert into \"%s\".\"%s\" ", this.getSchemaName(table, db),
			this.getPlainTableName(table)));
		List<String> nameList = new LinkedList<String>();
		List<String> valueList = new LinkedList<String>();
		for (String fieldName : document.getAllFields().keySet()) {
			Field field = document.getAllFields().get(fieldName);
			if (field == null) {
				continue;
			}
			nameList.add("\"" + field.getName() + "\"");
			valueList.add((field.getValue() == null) ? "null"
				: ("'"
					+ field.getValue().toString().replace("\\", "\\\\")
						.replace("'", "''") + "'"));
		}
		sb.append("(").append(StringUtils.join(nameList, ",")).append(") values (");
		sb.append(StringUtils.join(valueList, ",")).append(")");
		return sb.toString();
	}

	@Override
	public void deleteDocument(final String user, final String password,
		final String db, final String table, final String name)
		throws BackendException {
		Connection conn = null;
		Statement st = null;
		try {
			conn = this.connectToDB(user, password, db);
			st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_UPDATABLE);
			String queryString = String.format(
				"delete from \"%s\".\"%s\" where \"%s\"='%s'",
				this.getSchemaName(table, db), this.getPlainTableName(table),
				this.getNameField(conn, table, db), name);
			this.logString(queryString, "?");
			st.executeUpdate(queryString);
		} catch (SQLException e) {
			throw new BackendException(e);
		} catch (ClassNotFoundException e) {
			throw new BackendException(e);
		} finally {
			DbUtils.closeQuietly(st);
			DbUtils.closeQuietly(conn);
		}
	}

	@Override
	public Document getDocument(final String user, final String password,
		final String db, final String table, final String name,
		final boolean allowEmpty) throws BackendException {
		return this.getDocument(user, password, db, table, name, allowEmpty, null);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Document getDocument(final String user, final String password,
		final String db, final String table, final String name,
		final boolean allowEmpty, final Map<String, String[]> defaultFieldValues)
		throws BackendException {
		Connection conn = null;
		Statement st = null;
		ResultSet rs = null;
		try {
			conn = this.connectToDB(user, password, db);
			PostgresDocument doc = this.createEmptyDocument(conn, table, name, db);
			String nameField = this.getNameField(conn, table, db);
			st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
			String queryString = String.format(
				"select * from \"%s\".\"%s\" where \"%s\"='%s'",
				this.getSchemaName(table, db), this.getPlainTableName(table),
				nameField, name);
			this.logString(queryString, user);
			rs = st.executeQuery(queryString);
			if (this.getNumRows(rs) == 0) {
				if (allowEmpty) {
					if (defaultFieldValues != null) {
						for (String key : defaultFieldValues.keySet()) {
							Field field = doc.getAllFields().get(key);
							if (field != null) {
								if (field.getType() == FieldType.string
									|| field.getType() == FieldType.text) {
									field.setValue(defaultFieldValues.get(key)[0]);
								} else if (field.getType() == FieldType.date) {
									try {
										field.setValue(new SimpleDateFormat("y-M-d")
											.parse(defaultFieldValues.get(key)[0]));
									} catch (ParseException e) {
										throw new BackendException(e);
									}
								} else if (field.getType() == FieldType.dec) {
									field
										.setValue(Integer.valueOf(defaultFieldValues.get(key)[0]));
								} else if (field.getType() == FieldType.num) {
									field
										.setValue(Double.valueOf(defaultFieldValues.get(key)[0]));
								}
							}
						}
					}
					return doc;
				} else {
					throw new DocumentNotFoundException(String.format(
						"Document '%s' not found in table '%s.%s'.", name,
						this.getSchemaName(table, db), this.getPlainTableName(table)));
				}
			}
			rs.next();
			ResultSetMetaData md = rs.getMetaData();
			for (int i = 1; i <= md.getColumnCount(); i++) {
				Field field = doc.getAllFields().get(md.getColumnName(i));
				if (field.getType() == FieldType.string
					|| field.getType() == FieldType.text) {
					field.setValue(rs.getString(i));
				} else if (field.getType() == FieldType.date) {
					field.setValue(rs.getDate(i));
				} else if (field.getType() == FieldType.dec) {
					field.setValue(rs.getInt(i));
				} else if (field.getType() == FieldType.num) {
					field.setValue(rs.getDouble(i));
				}
			}
			return doc;
		} catch (SQLException e) {
			throw new BackendException(e);
		} catch (ClassNotFoundException e) {
			throw new BackendException(e);
		} finally {
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(st);
			DbUtils.closeQuietly(conn);
		}
	}

	@Override
	public Document saveDocument(final String user, final String password,
		final String db, final String table, final String name,
		final Document document) throws BackendException {
		Statement st = null;
		Connection conn = null;
		ResultSet rs = null;
		try {
			conn = this.connectToDB(user, password, db);
			String nameField = this.getNameField(conn, table, db);
			st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);

			rs = st.executeQuery(String.format(
				"select count(*) from \"%s\".\"%s\" where \"%s\"='%s'",
				this.getSchemaName(table, db), this.getPlainTableName(table),
				nameField, name));
			rs.next();
			int numEntries = rs.getInt(1);
			if (numEntries > 1) {
				throw new BackendException(
					String
						.format(
							"There are more than two entries with the name '%s' in the table '%s.%s', something is wrong with the database design.",
							name, this.getSchemaName(table, db),
							this.getPlainTableName(table)));
			}
			boolean isNewDocument = numEntries == 0 ? true : false;

			String insertOrUpdate = isNewDocument ? this.getInsertStatement(document,
				name, table, nameField, db) : this.getUpdateStatement(document, name,
				table, nameField, db);
			rs.close();
			if (!isNewDocument && document.getAllFields().containsKey("version")) {
				conn.setAutoCommit(false);
				st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
				String queryString = String.format(
					"select version from \"%s\".\"%s\" where \"%s\"='%s'",
					this.getSchemaName(table, db), this.getPlainTableName(table),
					nameField, name);
				this.logString(queryString, user);
				rs = st.executeQuery(queryString);
				rs.next();
				if (rs.getInt(1) != (Integer) document.getAllFields().get("version")
					.getValue()) {
					conn.rollback();
					throw new BackendException(
						String
							.format(
								"There is a new version of the record with name '%s' in table '%s.%s', please repeat editing.",
								name, this.getSchemaName(table, db),
								this.getPlainTableName(table)));
				}
				this.logString(insertOrUpdate, user);
				st.executeUpdate(insertOrUpdate);
				conn.commit();
				conn.setAutoCommit(true);
			} else {
				this.logString(insertOrUpdate, user);
				st.executeUpdate(insertOrUpdate);
			}
		} catch (SQLException e) {
			throw new BackendException(e);
		} catch (ClassNotFoundException e) {
			throw new BackendException(e);
		} finally {
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(st);
			DbUtils.closeQuietly(conn);
		}
		return document;
	}

	@Override
	public List<Row> executeQuery(final String user, final String password,
		final String db, final String query) throws BackendException {
		Statement st = null;
		Connection conn = null;
		ResultSet rs = null;
		List<Row> result = new LinkedList<Row>();
		try {
			conn = this.connectToDB(user, password, db);
			st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
			this.logString(query.trim(), user);
			rs = st.executeQuery(query.trim());
			while (rs.next()) {
				Row row = new PostgresRow();
				ResultSetMetaData metaData = rs.getMetaData();
				for (int i = 1; i <= metaData.getColumnCount(); i++) {
					row.getFields().add(rs.getObject(i));
					row.getFieldsByName().put(metaData.getColumnName(i), rs.getObject(i));
				}
				result.add(row);
			}
		} catch (SQLException e) {
			throw new BackendException(e);
		} catch (ClassNotFoundException e) {
			throw new BackendException(e);
		} finally {
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(st);
			DbUtils.closeQuietly(conn);
		}
		return result;
	}

	@Override
	public void executeUpdate(final String user, final String password,
		final String db, final String statement) throws BackendException {
		Statement st = null;
		Connection conn = null;
		try {
			conn = this.connectToDB(user, password, db);
			st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
			this.logString(statement.trim(), user);
			st.executeUpdate(statement.trim());
		} catch (SQLException e) {
			throw new BackendException(e);
		} catch (ClassNotFoundException e) {
			throw new BackendException(e);
		} finally {
			DbUtils.closeQuietly(st);
			DbUtils.closeQuietly(conn);
		}
	}

	@Override
	public List<String> getTables(final String user, final String password,
		final String db) throws BackendException {
		Statement st = null;
		Connection conn = null;
		ResultSet rs = null;
		String query = String
			.format(
				"select table_name from information_schema.tables where table_schema='%s'",
				this.getSchemaName("", db));
		List<String> result = new LinkedList<String>();
		try {
			conn = this.connectToDB(user, password, db);
			st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
			this.logString(query.trim(), user);
			rs = st.executeQuery(query.trim());
			while (rs.next()) {
				result.add(rs.getString(1));
			}
		} catch (SQLException e) {
			throw new BackendException(e);
		} catch (ClassNotFoundException e) {
			throw new BackendException(e);
		} finally {
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(st);
			DbUtils.closeQuietly(conn);
		}
		return result;
	}

	@Override
	public List<String> getNames(final String user, final String password,
		final String db, final String table) throws BackendException {
		Statement st = null;
		Connection conn = null;
		ResultSet rs = null;
		List<String> result = new LinkedList<String>();
		try {
			conn = this.connectToDB(user, password, db);
			st = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
				ResultSet.CONCUR_READ_ONLY);
			String query = String.format("select %s from \"%s\".\"%s\"",
				this.getNameField(conn, table, db), this.getSchemaName(table, db),
				this.getPlainTableName(table));
			this.logString(query.trim(), user);
			rs = st.executeQuery(query.trim());
			while (rs.next()) {
				result.add(rs.getString(1));
			}
		} catch (SQLException e) {
			throw new BackendException(e);
		} catch (ClassNotFoundException e) {
			throw new BackendException(e);
		} finally {
			DbUtils.closeQuietly(rs);
			DbUtils.closeQuietly(st);
			DbUtils.closeQuietly(conn);
		}
		return result;
	}
}
