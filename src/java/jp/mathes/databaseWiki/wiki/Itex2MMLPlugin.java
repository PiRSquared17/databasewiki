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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.mathes.databaseWiki.db.Backend;
import jp.mathes.databaseWiki.db.Document;
import jp.mathes.databaseWiki.db.Field;

public class Itex2MMLPlugin implements Plugin {

	private static final Pattern REGEX = Pattern.compile(
		"\\{math\\}.*?\\{/math\\}", Pattern.MULTILINE);

	@Override
	@SuppressWarnings("unchecked")
	public void process(final Document doc, final String fieldname,
		final String user, final String password, final Backend backend)
		throws PluginException {
		Field<String> field = doc.getAllFields().get(fieldname);
		String renderedText = field.getValue();
		Matcher matcher = Itex2MMLPlugin.REGEX.matcher(renderedText);
		while (matcher.find()) {
			String thisMathRegion = matcher.group(0).replace("{math}", "$");
			thisMathRegion = thisMathRegion.replace("{/math}", "$");
			Process process;
			try {
				process = Runtime.getRuntime().exec("itex2MML");
				process.getOutputStream().write(thisMathRegion.getBytes("UTF-8"));
				process.getOutputStream().flush();
				process.getOutputStream().close();
				BufferedReader stdout = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
				String line = null;
				StringBuilder sb = new StringBuilder();
				while ((line = stdout.readLine()) != null) {
					sb.append(line);
				}
				stdout.close();
				renderedText = renderedText.replace(matcher.group(0), sb.toString());
				process.destroy();
			} catch (IOException e) {
				throw new PluginException(e);
			}
		}
		field.setValue(renderedText);
	}
}
