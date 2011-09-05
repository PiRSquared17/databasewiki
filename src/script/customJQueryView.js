$.fn.dataTableExt.oApi.fnGetColumnData = function (oSettings, iColumn, bUnique, bFiltered, bIgnoreEmpty) {
  if ( typeof iColumn == "undefined" ) return new Array();
  if ( typeof bUnique == "undefined" ) bUnique = true;
  if ( typeof bFiltered == "undefined" ) bFiltered = true;
  if ( typeof bIgnoreEmpty == "undefined" ) bIgnoreEmpty = true;
  var aiRows;

  if (bFiltered == true) aiRows = oSettings.aiDisplay;
  else aiRows = oSettings.aiDisplayMaster; // all row numbers

  var asResultData = new Array();

  for (var i=0,c=aiRows.length; i<c; i++) {
    iRow = aiRows[i];
    var aData = this.fnGetData(iRow);
    var sValue = aData[iColumn];
    if (bIgnoreEmpty == true && sValue.length == 0) continue;
    else if (bUnique == true && jQuery.inArray(sValue, asResultData) > -1) continue;
    else asResultData.push(sValue);
  }
  return asResultData;
};

function stripTags(text) {return text.replace(/<\/?[^>]+>/gi, '');};

function fnCreateSelect(aData) {
  var r='<select><option value=""></option>', i, iLen=aData.length;
  for ( i=0 ; i<iLen ; i++ ) {
    r += '<option value="'+stripTags(aData[i])+'">'+stripTags(aData[i])+'</option>';
  }
  return r+'</select>';
}

$(document).ready(function() {
  $('table').each(function () {
    var oTable = $(this).dataTable({
      "oLanguage": {
        "sSearch": "Search all columns:"
      },
      "sPaginationType": "full_numbers",
      "bRetrieve" : true,
      "aaSorting": []
    });
    $('thead tr:last', oTable).after('<tr></tr>');
    $('thead tr:first th', oTable).each(function () {
      $('thead tr:last', oTable).append('<th></th>');
    });
    $("thead tr:last th", oTable).each( function (i) {
      this.innerHTML = fnCreateSelect(oTable.fnGetColumnData(i));
        $('select', this).change( function () {
          oTable.fnFilter( $(this).val(), i );
      });
    });
  });

  /* Add a select menu for each TH element in the table footer */
  // collapse lists
  $('li > ul, li > ol').each(function(i) {
      var parent_li = $(this).parent('li');
      parent_li.addClass('folder');
      var sub_ul = $(this).remove();
      parent_li.wrapInner('<a/>').find('a').click(function() {
        sub_ul.toggle();
      });
      parent_li.append(sub_ul);
  });
});