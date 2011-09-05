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
package jp.mathes.databaseWiki.db;

import java.util.List;
import java.util.Map;

public interface Backend {

	public static interface Row {
		List<Object> getFields();

		Map<String, Object> getFieldsByName();
	}

	Document getDocument(String user, String password, String db, String table,
		String name, boolean allowEmpty, Map<String, String[]> defaultFieldValues)
		throws BackendException, DocumentNotFoundException;

	Document getDocument(String user, String password, String db, String table,
		String name, boolean allowEmpty) throws BackendException,
		DocumentNotFoundException;

	Document saveDocument(String user, String password, String db, String table,
		String name, Document document) throws BackendException;

	void deleteDocument(String user, String password, String db, String table,
		String name) throws BackendException;

	List<Row> executeQuery(String user, String password, String db, String query)
		throws BackendException;

	void executeUpdate(String user, String password, String db, String statement)
		throws BackendException;

	List<String> getTables(String user, String password, String db)
		throws BackendException;

	List<String> getNames(String user, String password, String db, String table)
		throws BackendException;
}
