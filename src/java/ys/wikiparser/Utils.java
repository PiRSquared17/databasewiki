/*
 * Copyright (c) 2007 Yaroslav Stavnichiy, yarosla@gmail.com
 *
 * Latest version of this software can be obtained from:
 *   http://web-tec.info/WikiParser/
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * If you make use of this code, I'd appreciate hearing about it.
 * Comments, suggestions, and bug reports welcome: yarosla@gmail.com
 */

package ys.wikiparser;

import java.util.ArrayList;
import java.util.HashMap;

public class Utils {

	public static boolean isUrlChar(final char c) {
		// From MediaWiki: "._\\/~%-+&#?!=()@"
		// From http://www.ietf.org/rfc/rfc2396.txt :
		// reserved: ";/?:@&=+$,"
		// unreserved: "-_.!~*'()"
		// delim: "%#"
		if (Utils.isLatinLetterOrDigit(c)) {
			return true;
		}
		return "/?@&=+,-_.!~()%#;:$*".indexOf(c) >= 0; // I excluded '\''
	}

	public static boolean isLatinLetterOrDigit(final char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
			|| (c >= '0' && c <= '9');
	}

	/**
	 * Filters text so there are no '\r' chars in it ("\r\n" -&gt; "\n"; then "\r"
	 * -&gt; "\n"). Most importantly makes all blank lines (lines with only
	 * spaces) exactly like this: "\n\n". WikiParser relies on that.
	 * 
	 * @param text
	 * @return filtered text
	 */
	public static String preprocessWikiText(String text) {
		if (text == null) {
			return "";
		}
		text = text.trim();
		int length = text.length();
		char[] chars = new char[length];
		text.getChars(0, length, chars, 0);
		StringBuilder sb = new StringBuilder();
		boolean blankLine = true;
		StringBuilder spaces = new StringBuilder();
		for (int p = 0; p < length; p++) {
			char c = chars[p];
			if (c == '\r') { // "\r\n" -> "\n"; then "\r" -> "\n"
				if (p + 1 < length && chars[p + 1] == '\n') {
					p++;
				}
				sb.append('\n');
				spaces.delete(0, spaces.length()); // discard spaces if there is nothing
																						// else on the line
				blankLine = true;
			} else if (c == '\n') {
				sb.append(c);
				spaces.delete(0, spaces.length()); // discard spaces if there is nothing
																						// else on the line
				blankLine = true;
			} else if (blankLine) {
				if (c <= ' '/* && c!='\n' */) {
					spaces.append(c);
				} else {
					sb.append(spaces);
					blankLine = false;
					sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String escapeHTML(final String s) {
		if (s == null) {
			return "";
		}
		StringBuffer sb = new StringBuffer(s.length() + 100);
		int length = s.length();

		for (int i = 0; i < length; i++) {
			char ch = s.charAt(i);

			if ('<' == ch) {
				sb.append("&lt;");
			} else if ('>' == ch) {
				sb.append("&gt;");
			} else if ('&' == ch) {
				sb.append("&amp;");
			} else if ('\'' == ch) {
				sb.append("&#39;");
			} else if ('"' == ch) {
				sb.append("&quot;");
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	private static HashMap<String, Character> entities = null;

	private static synchronized HashMap<String, Character> getHtmlEntities() {
		if (Utils.entities == null) {
			Utils.entities = new HashMap<String, Character>();
			Utils.entities.put("lt", '<');
			Utils.entities.put("gt", '>');
			Utils.entities.put("amp", '&');
			Utils.entities.put("quot", '"');
			Utils.entities.put("apos", '\'');
			Utils.entities.put("nbsp", '\u00A0');
			Utils.entities.put("shy", '\u00AD');
			Utils.entities.put("copy", '\u00A9');
			Utils.entities.put("reg", '\u00AE');
			Utils.entities.put("trade", '\u2122');
			Utils.entities.put("mdash", '\u2014');
			Utils.entities.put("ndash", '\u2013');
			Utils.entities.put("ldquo", '\u201C');
			Utils.entities.put("rdquo", '\u201D');
			Utils.entities.put("euro", '\u20AC');
			Utils.entities.put("middot", '\u00B7');
			Utils.entities.put("bull", '\u2022');
			Utils.entities.put("laquo", '\u00AB');
			Utils.entities.put("raquo", '\u00BB');
		}
		return Utils.entities;
	}

	public static String unescapeHTML(final String value) {
		if (value == null) {
			return null;
		}
		if (value.indexOf('&') < 0) {
			return value;
		}
		HashMap<String, Character> ent = Utils.getHtmlEntities();
		StringBuffer sb = new StringBuffer();
		final int length = value.length();
		for (int i = 0; i < length; i++) {
			char c = value.charAt(i);
			if (c == '&') {
				char ce = 0;
				int i1 = value.indexOf(';', i + 1);
				if (i1 > i && i1 - i <= 12) {
					if (value.charAt(i + 1) == '#') {
						if (value.charAt(i + 2) == 'x') {
							ce = (char) Utils.atoi(value.substring(i + 3, i1), 16);
						} else {
							ce = (char) Utils.atoi(value.substring(i + 2, i1));
						}
					} else {
						synchronized (ent) {
							Character ceObj = ent.get(value.substring(i + 1, i1));
							ce = ceObj == null ? 0 : ceObj.charValue();
						}
					}
				}
				if (ce > 0) {
					sb.append(ce);
					i = i1;
				} else {
					sb.append(c);
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	static public int atoi(final String s) {
		try {
			return Integer.parseInt(s);
		} catch (Throwable ex) {
			return 0;
		}
	}

	static public int atoi(final String s, final int base) {
		try {
			return Integer.parseInt(s, base);
		} catch (Throwable ex) {
			return 0;
		}
	}

	public static String replaceString(final String str, final String from,
		final String to) {
		StringBuffer buf = new StringBuffer();
		int flen = from.length();
		int i1 = 0, i2 = 0;
		while ((i2 = str.indexOf(from, i1)) >= 0) {
			buf.append(str.substring(i1, i2));
			buf.append(to);
			i1 = i2 + flen;
		}
		buf.append(str.substring(i1));
		return buf.toString();
	}

	public static String[] split(final String s, final char separator) {
		// this is meant to be faster than String.split() when separator is not
		// regexp
		if (s == null) {
			return null;
		}
		ArrayList<String> parts = new ArrayList<String>();
		int beginIndex = 0, endIndex;
		while ((endIndex = s.indexOf(separator, beginIndex)) >= 0) {
			parts.add(s.substring(beginIndex, endIndex));
			beginIndex = endIndex + 1;
		}
		parts.add(s.substring(beginIndex));
		String[] a = new String[parts.size()];
		return parts.toArray(a);
	}

	private static final String translitTable = "�a�b�v�g�d�e�e�zh�z�i�y�k�l�m�n�o�p�r�s�t�u�f�h�ts�ch�sh�sch��y��e�yu�ya�A�B�V�G�D�E�E�ZH�Z�I�Y�K�L�M�N�O�P�R�S�T�U�F�H�TS�CH�SH�SCH��Y��E�YU�YA";

	/**
	 * Translates all non-basic-latin-letters characters into latin ones for use
	 * in URLs etc. Here is the implementation for cyrillic (Russian) alphabet.
	 * Unknown characters are omitted.
	 * 
	 * @param s
	 *          string to be translated
	 * @return translated string
	 */
	public static String translit(final String s) {
		if (s == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(s.length() + 100);
		final int length = s.length();
		final int translitTableLength = Utils.translitTable.length();

		for (int i = 0; i < length; i++) {
			char ch = s.charAt(i);
			// System.err.println("ch="+(int)ch);

			if ((ch >= '�' && ch <= '�') || (ch >= '�' && ch <= '�') || ch == '�'
				|| ch == '�') {
				int idx = Utils.translitTable.indexOf(ch);
				char c;
				if (idx >= 0) {
					for (idx++; idx < translitTableLength; idx++) {
						c = Utils.translitTable.charAt(idx);
						if ((c >= '�' && c <= '�') || (c >= '�' && c <= '�') || c == '�'
							|| c == '�') {
							break;
						}
						sb.append(c);
					}
				}
			} else {
				sb.append(ch);
			}
		}
		return sb.toString();
	}

	public static String emptyToNull(final String s) {
		return "".equals(s) ? null : s;
	}

	public static String noNull(final String s) {
		return s == null ? "" : s;
	}

	public static String noNull(final String s, final String val) {
		return s == null ? val : s;
	}

	public static boolean isEmpty(final String s) {
		return (s == null || s.length() == 0);
	}
}
