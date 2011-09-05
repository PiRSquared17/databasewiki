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

import com.codegremlins.jurlmap.servlet.DispatchFilter;

public class DbwDispatchFilter extends DispatchFilter {

	@Override
	protected void configure() {
		this.forward("/dbwservlet", "/$_db/");
		this.forward("/dbwservlet", "/$_db/$_table");
		this.forward("/dbwservlet", "/$_db/$_table/$name/[edit|view|delete]_action?");
		this.forward("/dbwservlet", "/$_db/$_table/$name/[save]_action;POST");
		this.forward("/dbwservlet", "/$_db/$_table/_new");
		this.forward("/dbwservlet", "/[sql]_action;POST");
	}
}
