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
package jp.mathes.databaseWiki.web;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import jp.mathes.databaseWiki.db.Backend;
import jp.mathes.databaseWiki.wiki.Plugin;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class DbwConfiguration {

	static private DbwConfiguration instance = null;
	private Backend backend;
	private List<Plugin> plugins = new LinkedList<Plugin>();
	private File davLogFile = null;

	static public DbwConfiguration getInstance() {
		if (DbwConfiguration.instance == null) {
			synchronized (DbwConfiguration.class) {
				if (DbwConfiguration.instance == null) {
					DbwConfiguration.instance = new DbwConfiguration();
				}
			}
		}
		return DbwConfiguration.instance;
	}

	public DbwConfiguration() {
		InputStream resourceStream = null;
		try {
			Properties props = new Properties();
			resourceStream = this.getClass().getResourceAsStream("/dbw.properties");
			props.load(resourceStream);
			String backendClassName = props.getProperty("dbw.db.backend");
			this.backend = (Backend) Class.forName(backendClassName).newInstance();
			String davLogFileName = props.getProperty("dbw.dav.log");
			if (!StringUtils.isEmpty(davLogFileName)) {
				this.davLogFile = new File(davLogFileName);
			}
			this.plugins.clear();
			String pluginString = props.getProperty("dbw.wiki.plugins");
			for (String pluginName : pluginString.split(",")) {
				if (StringUtils.isEmpty(pluginName)) {
					continue;
				}
				Plugin thisPlugin = (Plugin) Class.forName(
					"jp.mathes.databaseWiki.wiki." + pluginName.trim()).newInstance();
				if (thisPlugin != null) {
					this.plugins.add(thisPlugin);
				}
			}
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(resourceStream);
		}
	}

	public Backend getBackend() {
		return this.backend;
	}

	public List<Plugin> getPlugins() {
		return this.plugins;
	}

	public void davLog(String message) {
		davLog(message, null);
	}

	public void davLog(String message, Throwable e) {
		if (this.davLogFile != null) {
			try {
				FileUtils.write(this.davLogFile, message, true);
				FileUtils.write(this.davLogFile, "\n", true);
				if (e != null) {
					FileUtils.write(this.davLogFile, ExceptionUtils.getStackTrace(e),
						true);
					FileUtils.write(this.davLogFile, "\n", true);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
}
