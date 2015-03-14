databaseWiki (dbw) is a wiki engine for structured and unstructured data that is just a small layer over the database. Basically it displays (and edits) entries/rows (containing multiple fields) of a database table on the web. Fields that contain wiki text are rendered and it is possible that this wiki text contains embedded SQL which is either a query whose result is displayed or an action that is executed by pressing a button. Please not that authentication and authorization have to be done in the underlying database, dbw uses the credentials it asks for to connect to the database.

With dbw it is very easy to manage structured data on the web. It currently only supports PostgreSQL as database, but other backend could be written and activated in the dbw.properties file. It currently has more the status of a proof of concept, I use it personally and maybe it is useful to somebody else. There are no releases, you have to check out the source and call 'ant build', that compiles everything to the 'war' directory, which you can link/copy to the webapps directory of a Tomcat (or potentially any other servlet container, but I only tried it with Apache Tomcat).

If you want to address a single record in the database you can use a URLs like

<pre>/<servletContext>/<database>/<table>/<recordName></pre>

e.g. if databaseWiki is in the servlet context dbw and you have a database 'wiki' with a table 'todo' in it, then

<pre>http://<host:port>/dbw/wiki/todo/writeBetterDocsForDbw</pre>

could be a URL that displays one record in the todo table. Which column is used as name (here writeBetterDocsForDbw) depends on the backend, the postgresql backend uses the first primary key column for that (there may be problem if you primary key spans more than one column). In the postgresql backend you can also give the table name as 'schema.table', if no schema is given, it defaults to 'public'. It also depends on the backend which columns may contain wiki markup. For the postgresql backend that is all columns of type 'TEXT' (while columns of type VARCHAR are just pure text without markup). The wiki markup is basically wikicreole (parsed by the T4 wiki creole parser), but enhanced with some plugins to e.g. embed SQL queries. You can write additional wiki plugins and configure them in dbw.properties. In some plugins the Freemarker template engine is used to e.g. iterate over the result of an SQL query.

If you, for example, want to show a (html) table with all todos, you can put the following code into a wiki field (a field with type TEXT in case you use the postgresql backend):

<pre>
{sql type="loop"}<br>
{query}<br>
select * from todo order by enddate desc, priority asc<br>
{/query}<br>
{body}<br>
|=name|=start|=end|=priority|=description|<br>
[#list rows as row]<br>
|{[todo:${row.fieldsByName.name}=${row.fieldsByName.name}]}|${(row.fieldsByName.startdate?string("yyyy-MM-dd"))!" "}|${(row.fieldsByName.enddate?string("yyyy-MM-dd"))!" "}|${row.fieldsByName.priority!" "}|${row.fieldsByName.content!" ", 150)}|<br>
[/#list]<br>
{/body}<br>
{/sql}<br>
</pre>

it uses Freemarker to access the result of the query in the variable 'rows' and creates wiki markup of the table. If the table is displayed on the web, the jQuery plugin dataTable is used to provide navigation, search and filtering. However, the style/appearance of the page may look a little simple and ugly, you probably want to adapt dbw.css, if you understand more about web design than me.

The URL `/<servletContext>/<database>/<table>/<recordName>` redirects to `/<servletContext>/<database>/<table>/<recordName>/view`, the alternatives to `/view` are `/edit` and `/delete`. SQL query and actions are handled by the SQLLoopPlugin and the SQLButtonPlugin. Other plugins are DbwLinkPlugin to parse intra-Wiki links of the form `{[db$table:name=desc]}` (you can leave out db and table if it is the same than the current page and you can leave out the description if you are fine with the name as link label), Itex2MMLPlugin (renders mathematical formulas in Latex syntax with the external program itex2MML, which has to be installed for the plugin to work), NewPlugin which provides a text field and button to create new records and WikiCreolePlugin that renders the wiki markup. Please note that the order of configured plugins is relevant, e.g. if you want to create wiki markup with the result of an SQL query (as in the example above), SQLLoopPlugin hast to run before WikiCreolePlugin (that is how the default dbw.properties is written).

Please also note that dbw has no capabilities of creating databases or tables, you have to do that with a different tool.

One additional thing that dbw provides is a webdav interface to the database. It is not terrible fast, if you want to use it on a large database, you probably have to optimize it with caches etc. The URL for a database is `/_/dav/<database>`, you can also view a single table with `/_/dav/<database>/<table>`. You can view (the source, i.e. the wiki plugins are not executed), edit and delete entries. You cannot copy or move records/files as other tables typically have another structure and therefore moving records between tables does not make any sense.

Having a webdav interface allows some interesting use cases, e.g. you can use the webdav interface to backup your data or copy it to another computer. Or you could even use a distributed version control system like mercurial to sync multiple instances of dbw.

If you have any questions feel free to ask or create an issue.