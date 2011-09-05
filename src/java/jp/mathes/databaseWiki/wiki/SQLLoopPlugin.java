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
package jp.mathes.databaseWiki.wiki;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.mathes.databaseWiki.db.Backend;
import jp.mathes.databaseWiki.db.Backend.Row;
import jp.mathes.databaseWiki.db.BackendException;
import jp.mathes.databaseWiki.db.Document;
import jp.mathes.databaseWiki.db.Field;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Parses embedded SQL of type
 * 
 * {sql type=loop} {query}[SQL query with freemarker variables]{/query}
 * {body}[wiki body to render]{/body} {/sql}
 * 
 */
public class SQLLoopPlugin implements Plugin {

	private static final Pattern REGEX = Pattern
		.compile(
			"\\{sql\\s+type=\"loop\"\\}\\s*\\{query\\}(.*?)\\{/query\\}\\s*\\{body\\}(.*?)\\{/body\\}\\s*\\{/sql\\}",
			Pattern.MULTILINE | Pattern.DOTALL);

	@SuppressWarnings("unchecked")
	@Override
	public void process(final Document doc, final String fieldname,
		final String user, final String password, final Backend backend)
		throws PluginException {
		Field<String> field = doc.getAllFields().get(fieldname);
		String renderedText = field.getValue();
		if (renderedText == null) {
			return;
		}
		Configuration conf = new Configuration();
		conf.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
		Matcher loopMatcher = SQLLoopPlugin.REGEX.matcher(renderedText);
		while (loopMatcher.find()) {
			String query = loopMatcher.group(1);
			String body = loopMatcher.group(2);
			Template template;
			try {
				template = new Template("name", new StringReader(query), conf);
				HashMap<String, Object> data = new HashMap<String, Object>();
				data.put("doc", doc);
				data.put("fields", doc.getAllFields());
				StringWriter sw = new StringWriter();
				template.process(data, sw);
				query = sw.getBuffer().toString();
				List<Row> rows = backend.executeQuery(user, password,
					doc.getDatabase(), query);
				template = new Template("name", new StringReader(body), conf);
				data.put("rows", rows);
				sw = new StringWriter();
				template.process(data, sw);
				renderedText = renderedText
					.replace(loopMatcher.group(0), sw.toString());
				field.setValue(renderedText);
			} catch (IOException e) {
				throw new PluginException(e);
			} catch (TemplateException e) {
				throw new PluginException(e);
			} catch (BackendException e) {
				throw new PluginException(e);
			}
		}
	}
}
