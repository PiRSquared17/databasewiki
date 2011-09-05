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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.mathes.databaseWiki.db.Backend;
import jp.mathes.databaseWiki.db.Document;
import jp.mathes.databaseWiki.db.Field;

import org.apache.commons.lang3.StringUtils;

/**
 * matches {[db$table:name=desc]}, {[table:name=desc]}, {[name=desc]} and these
 * patterns without description and puts db, table, name, desc in groups
 */
public class DbwLinkPlugin implements Plugin {

	private static final Pattern REGEX = Pattern
		.compile("\\{\\[(?:(?:([\\w]*)\\$)?([\\w]*):)?([\\w]*)(?:\\=([\\w\\s]+))?]}");

	@Override
	@SuppressWarnings("unchecked")
	public void process(final Document doc, final String fieldname,
		final String user, final String password, final Backend backend) {
		Field<String> field = doc.getAllFields().get(fieldname);
		String renderedText = field.getValue();
		Matcher matcher = DbwLinkPlugin.REGEX.matcher(renderedText);
		while (matcher.find()) {
			String path = "";
			String prefix = ".";
			String description = matcher.group(4);
			if (StringUtils.isEmpty(matcher.group(1))) {
				if (StringUtils.isEmpty(matcher.group(2))) {
					path = matcher.group(3);
					prefix = "..";
				} else {
					path = String.format("%s/%s", matcher.group(2), matcher.group(3));
					prefix = "../..";
				}
			} else {
				path = String.format("%s/%s/%s", matcher.group(1), matcher.group(2),
					matcher.group(3));
				prefix = "../../..";
			}
			renderedText = renderedText.replace(matcher.group(0), String.format(
				"<a href=\"%s/%s/view\">%s</a>", prefix, path,
				StringUtils.isEmpty(description) ? path : description));
		}
		field.setValue(renderedText);
	}
}
