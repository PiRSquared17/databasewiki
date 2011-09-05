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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.mathes.databaseWiki.db.Backend;
import jp.mathes.databaseWiki.db.Document;
import jp.mathes.databaseWiki.db.Field;

import org.apache.commons.lang3.StringEscapeUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Parses embedded SQL of the type
 * 
 * {sql type=button}[SQL query]{/sql}
 * 
 * The SQLServlet is used to execute actions from buttons
 * 
 */
public class SQLButtonPlugin implements Plugin {

	private static final Pattern REGEX = Pattern.compile(
		"\\{sql\\s+type=&quot;button&quot;\\}\\s*(.*?)\\s*\\{/sql\\}",
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
		Matcher buttonMatcher = SQLButtonPlugin.REGEX.matcher(renderedText);
		while (buttonMatcher.find()) {
			String statement = buttonMatcher.group(1);
			statement = statement.replace("&quot;", "\"").replace("&#39;", "'");
			Template template;
			try {
				template = new Template("name", new StringReader(statement), conf);
				HashMap<String, Object> data = new HashMap<String, Object>();
				data.put("doc", doc);
				data.put("fields", doc.getAllFields());
				StringWriter sw = new StringWriter();
				template.process(data, sw);
				statement = sw.getBuffer().toString();
				StringBuilder sb = new StringBuilder(
					"<form class=\"execute\" method=\"post\" action=\"../../../sql\">");
				sb.append("<input type=\"hidden\" name=\"statement\" value=\"")
					.append(StringEscapeUtils.escapeXml(statement))
					.append("\" />")
					.append("<input type=\"hidden\" name=\"_db\" value=\"")
					.append(doc.getDatabase())
					.append("\" />")
					.append("<input type=\"hidden\" name=\"_table\" value=\"")
					.append(doc.getTable())
					.append("\" />")
					.append("<input type=\"hidden\" name=\"name\" value=\"")
					.append(doc.getName())
					.append("\" />")
					.append(
						"<input type=\"submit\" name=\"execute\" value=\"execute\" />")
					.append("</form>");
				renderedText = renderedText.replace(buttonMatcher.group(0),
					sb.toString());
				field.setValue(renderedText);
			} catch (TemplateException e) {
				throw new PluginException(e);
			} catch (IOException e) {
				throw new PluginException(e);
			}
		}
	}
}
