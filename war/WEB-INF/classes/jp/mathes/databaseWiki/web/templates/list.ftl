<#compress>
<?xml version="1.0" encoding="utf-8" ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN" "http://www.w3.org/Math/DTD/mathml2/xhtml-math11-f.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title>${title}</title>
  <meta http-equiv="Content-Type" content="application/xhtml+xml; charset=utf-8" />
  <link rel="icon" href="images/favicon.ico" type="image/x-icon" />
</head>
  <body>
    <#list urls as url>
      <a href="${context}/${url}">${url}</a><br/>
    </#list>
  </body>
</html>
</#compress>