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

import org.apache.commons.lang3.StringUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * Parses embedded new actions
 * 
 * <pre>
 * {new db="[db]" table="[table]"}
 *   field1=value1,field2=value2,
 *   field3=value3,field4=value4
 * {/new}
 * </pre>
 * 
 * value may include freemarker variables of the current document backslashes
 * may be used to escape commas in the values (but nothing else)
 * 
 */
public class NewPlugin implements Plugin {

	private static final Pattern REGEX = Pattern
		.compile(
			"\\{new\\s+(?:db=&quot;([^&]*)&quot;\\s+)?table=&quot;([^&]*)&quot;\\s*\\}(.*)\\{/new\\}",
			Pattern.MULTILINE | Pattern.DOTALL);

	@SuppressWarnings("unchecked")
	@Override
	public void process(final Document doc, final String fieldname,
		final String user, final String password, final Backend backend)
		throws PluginException {
		Field<String> field = doc.getAllFields().get(fieldname);
		String renderedText = field.getValue();
		Matcher matcher = NewPlugin.REGEX.matcher(renderedText);
		while (matcher.find()) {
			String target = "";
			String param = "";
			if (StringUtils.isEmpty(matcher.group(1))) {
				target = String.format("../../%s/_new", matcher.group(2));
				param = matcher.group(3);
			} else {
				target = String.format("../../../%s/%s/_new", matcher.group(1),
					matcher.group(2));
				param = matcher.group(3);
			}
			try {
				Configuration conf = new Configuration();
				conf.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
				Template template = new Template("name", new StringReader(param),
					new Configuration());
				HashMap<String, Object> data = new HashMap<String, Object>();
				data.put("doc", doc);
				data.put("fields", doc.getAllFields());
				StringWriter sw = new StringWriter();
				template.process(data, sw);
				param = sw.getBuffer().toString();
			} catch (IOException e) {
				throw new PluginException(e);
			} catch (TemplateException e) {
				throw new PluginException(e);
			}
			// match a comma except if it is preceeded by a backslash
			String[] split = param.split("(?<!\\\\),");
			StringBuilder sbForm = new StringBuilder(
				"<form method=\"post\" class=\"new\" action=\"%s\"><input type=\"text\" name=\"name\" />");
			for (String oneParameter : split) {
				String[] oneParameterSplit = StringUtils.split(oneParameter,
					"(?<!\\\\)=", 2);
				if (oneParameterSplit.length == 2) {
					sbForm.append(String.format(
						"<input type=\"hidden\" name=\"%s\" value=\"%s\"/>",
						oneParameterSplit[0], oneParameterSplit[1]));
				}
			}
			sbForm.append("<input type=\"submit\" value=\"create\"/></form>");
			String formString = String.format(sbForm.toString(), target);
			renderedText = renderedText.replace(matcher.group(0), formString);
			field.setValue(renderedText);
		}
	}
}
