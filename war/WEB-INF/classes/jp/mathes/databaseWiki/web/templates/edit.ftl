<?xml version="1.0" encoding="utf-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1//EN" "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${title}</title>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
  <link href="${context}/_/style/edit-min.css" rel="stylesheet" type="text/css" />
  <link rel="icon" href="${context}/_/images/favicon.ico" type="image/x-icon" />
</head>
  <body>
    <form action="save" method="post">
    <div id="actions">
      <input type="submit" name="Save" value="Save" />
    </div>
    <div id="fields">
    <#list fields?keys as fieldkey>
      <#assign field=fields[fieldkey]>
      <#if field.usage == FieldUsage.hidden>
        <input type="hidden" name="${field.name}" value="${field.value!""}"/>
      <#elseif field.usage == FieldUsage.fixed>
        <div class="select_field"><label class="main">${field.name}</label>
        <select name="fields.${field.name}" class="main">
          <#list field.allowedValues as allowedValue>
          <#if allowedValue == field.value!"">
            <option selected="true">${allowedValue}</option>
          <#else>
            <option>${allowedValue}</option>
          </#if>
          </#list>
        </select>
        </div>
      <#elseif field.type == FieldType.string>
        <div class="string_field"><label class="main">${field.name}</label><input class="main" type="text" name="fields.${field.name}" value="${(field.value!"")?xhtml}"/></div>
      <#elseif field.type == FieldType.text && field.name?lower_case != "Content"?lower_case>
        <div class="text_field"><label class="main">${field.name}</label><textarea class="main" name="fields.${field.name}">${(field.value!"")?xhtml}</textarea></div>
      <#elseif field.type == FieldType.text && field.name?lower_case == "Content"?lower_case>
        <div id="textcontentEdit">
          <textarea name="fields.${field.name}">${(field.value!"")?xhtml}</textarea>
        </div>
      <#elseif field.type == FieldType.date>
        <div class="date_field"><label class="main">${field.name}</label><input class="main" type="text" name="fields.${field.name}" value="<#if field.value??>${field.value?date?string("yyyy-MM-dd")}</#if>"/> <span class="main">yyyy-mm-dd</span></div>
      <#elseif field.type == FieldType.dec>
        <div class="dec_field"><label class="main">${field.name}</label><input class="main" type="text" name="fields.${field.name}" value="${field.value!""}"/> <span class="main">xxx</span></div>
      <#elseif field.type == FieldType.num>
        <div class="num_field"><label class="main">${field.name}</label><input class="main" type="text" name="fields.${field.name}" value="<#if field.value??>${field.value?c}</#if>"/> <span class="main">xxx.xxx</span></div>
      </#if>
    </#list>
    </div>
    </form>
    <script type="text/javascript" src="${context}/_/script/edit.js" />
  </body>
</html>
