<#list fields?keys as fieldkey>
  <#assign field=fields[fieldkey]>
  <#if field.usage == FieldUsage.hidden>
  <#elseif field.type == FieldType.string>
${field.name}: ${(field.value!"")}
  <#elseif field.type == FieldType.text && field.name?lower_case != "Content"?lower_case>
${field.name}: ${field.value!""?replace("\n","\n\t")}
  <#elseif field.type == FieldType.date>
${field.name}: <#if field.value??>${field.value?date?string("yyyy-MM-dd")}</#if>
  <#elseif field.type == FieldType.dec>
${field.name}: ${field.value!""}
  <#elseif field.type == FieldType.num>
${field.name}: <#if field.value??>${field.value?c}</#if>
  <#elseif field.type == FieldType.text && field.name?lower_case == "Content"?lower_case>
  
${field.value!""}
  </#if>
</#list>