<#compress>
<?xml version="1.0" encoding="utf-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN" "http://www.w3.org/Math/DTD/mathml2/xhtml-math11-f.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${title}</title>
  <meta http-equiv="Content-Type" content="application/xhtml+xml; charset=utf-8" />
  <link href="${context}/_/style/view-min.css" rel="stylesheet" type="text/css" />
  <link rel="icon" href="${context}/_/images/favicon.ico" type="image/x-icon" />
</head>
  <body>
    <div id="fields">
    <#list fields?keys as fieldkey>
      <#assign field=fields[fieldkey]>
      <#if field.usage == FieldUsage.hidden>
      <#elseif field.type == FieldType.string>
        <div class="string_field"><label class="main">${field.name}:&nbsp;</label>${(field.value!"")?xhtml}</div>
      <#elseif field.type == FieldType.text && field.name?lower_case != "Content"?lower_case>
        <div class="text_field"><label class="main">${field.name}:&nbsp;</label>${field.value!""}</div>
      <#elseif field.type == FieldType.text && field.name?lower_case == "Content"?lower_case>
        <div id="textcontent">
          ${field.value!""}
        </div>
      <#elseif field.type == FieldType.date>
        <div class="date_field"><label class="main">${field.name}:&nbsp;</label><#if field.value??>${field.value?date?string.long}</#if></div>
      <#elseif field.type == FieldType.dec>
        <div class="dec_field"><label class="main">${field.name}:&nbsp;</label>${field.value!""}</div>
      <#elseif field.type == FieldType.num>
        <div class="num_field"><label class="main">${field.name}:&nbsp;</label><#if field.value??>${field.value?c}</#if></div>
      </#if>
    </#list>
    </div>
    <script type="text/javascript" src="${context}/_/script/view.js" />
  </body>
</html>
</#compress>