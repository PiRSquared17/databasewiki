/*
 * Copyright 2007-2009 Yaroslav Stavnichiy, yarosla@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Latest version of this software can be obtained from:
 *
 *     http://t4-wiki-parser.googlecode.com/
 *
 * If you make use of this code, I'd appreciate hearing about it.
 * Comments, suggestions, and bug reports welcome: yarosla@gmail.com
 */

package ys.wikiparser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

/**
 * WikiParser.renderXHTML() is the main method of this class. It takes wiki-text
 * and returns XHTML.
 * 
 * WikiParser's behavior can be customized by overriding appendXxx() methods,
 * which should make integration of this class into any wiki/blog/forum software
 * easy and painless.
 * 
 * @author Yaroslav Stavnichiy (yarosla@gmail.com)
 * 
 */
public class WikiParser {

	private int wikiLength;
	private char wikiChars[];
	protected StringBuilder sb = new StringBuilder();
	protected StringBuilder toc = new StringBuilder();
	protected int tocLevel = 0;
	private HashSet<String> tocAnchorIds = new HashSet<String>();
	private String wikiText;
	private int pos = 0;
	private int listLevel = -1;
	private static final int MAX_LIST_LEVELS = 100;
	private char listLevels[] = new char[WikiParser.MAX_LIST_LEVELS + 1]; // max
																																				// number
																																				// of
																																				// levels
																																				// allowed
	private boolean blockquoteBR = false;
	private boolean inTable = false;
	private boolean inHead = false;
	private int mediawikiTableLevel = 0;

	protected int HEADING_LEVEL_SHIFT = 1; // make =h2, ==h3, ...
	protected String HEADING_ID_PREFIX = null;

	private static enum ContextType {
		PARAGRAPH, LIST_ITEM, TABLE_CELL, HEADER, NOWIKI_BLOCK
	};

	private static final String[] ESCAPED_INLINE_SEQUENCES = { "{{{", "{{",
		"}}}", "**", "//", "__", "##", "\\\\", "[[", "<<<", "~", "--", "|" };

	private static final String LIST_CHARS = "*-#>:!";
	private static final String[] LIST_OPEN = { "<ul><li>", "<ul><li>",
		"<ol><li>", "<blockquote>", "<div class='indent'>", "<div class='center'>" };
	private static final String[] LIST_CLOSE = { "</li></ul>\n", "</li></ul>\n",
		"</li></ol>\n", "</blockquote>\n", "</div>\n", "</div>\n" };

	private static final String FORMAT_CHARS = "*/_#";
	private static final String[] FORMAT_DELIM = { "**", "//", "__", "##" };
	private static final String[] FORMAT_TAG_OPEN = { "<strong>", "<em>",
		"<span class=\"underline\">", "<tt>" };
	private static final String[] FORMAT_TAG_CLOSE = { "</strong>", "</em>",
		"</span>", "</tt>" };

	public static String renderXHTML(final String wikiText) {
		return new WikiParser(wikiText).toString();
	}

	protected void parse(String wikiText) {
		wikiText = Utils.preprocessWikiText(wikiText);

		this.wikiText = wikiText;
		this.wikiLength = this.wikiText.length();
		this.wikiChars = new char[this.wikiLength];
		this.wikiText.getChars(0, this.wikiLength, this.wikiChars, 0);

		while (this.parseBlock()) {
			;
		}

		this.closeListsAndTables();

		while (this.mediawikiTableLevel-- > 0) {
			this.sb.append("</td></tr></tbody></table>\n");
		}

		this.completeTOC();
	}

	protected WikiParser() {
		// for use by subclasses only
		// subclasses should call parse() to complete construction
	}

	protected WikiParser(final String wikiText) {
		this.parse(wikiText);
	}

	@Override
	public String toString() {
		return this.sb.toString();
	}

	private void closeListsAndTables() {
		// close unclosed lists
		while (this.listLevel >= 0) {
			this.sb.append(WikiParser.LIST_CLOSE[WikiParser.LIST_CHARS
				.indexOf(this.listLevels[this.listLevel--])]);
		}
		if (this.inTable) {
			this.sb.append("</tbody></table>\n");
			this.inTable = false;
		}
	}

	private boolean parseBlock() {
		for (; this.pos < this.wikiLength && this.wikiChars[this.pos] <= ' '
			&& this.wikiChars[this.pos] != '\n'; this.pos++) {
			; // skip whitespace
		}
		if (this.pos >= this.wikiLength) {
			return false;
		}

		char c = this.wikiChars[this.pos];

		if (c == '\n') { // blank line => end of list/table; no other meaning
			this.closeListsAndTables();
			this.pos++;
			return true;
		}

		if (c == '|') { // table
			if (this.mediawikiTableLevel > 0) {
				int pp = this.pos + 1;
				if (pp < this.wikiLength) {
					boolean newRow = false, endTable = false;
					if (this.wikiChars[pp] == '-') { // mediawiki-table new row
						newRow = true;
						pp++;
					} else if (this.wikiChars[pp] == '}') { // mediawiki-table end table
						endTable = true;
						pp++;
					}
					for (; pp < this.wikiLength
						&& (this.wikiChars[pp] == ' ' || this.wikiChars[pp] == '\t'); pp++) {
						; // skip spaces
					}
					if (pp == this.wikiLength || this.wikiChars[pp] == '\n') { // nothing
																																			// else on
																																			// the
																																			// line =>
																																			// it's
																																			// mediawiki-table
																																			// markup
						this.closeListsAndTables(); // close lists if any
						this.sb.append(newRow ? "</td></tr>\n<tr><td>"
							: (endTable ? "</td></tr></tbody></table>\n" : "</td>\n<td>"));
						if (endTable) {
							this.mediawikiTableLevel--;
						}
						this.pos = pp + 1;
						return pp < this.wikiLength;
					}
				}
			}

			if (!this.inTable) {
				this.closeListsAndTables(); // close lists if any
				this.sb.append("<table border=\"1\"><thead>");
				this.inHead = true;
				this.inTable = true;
			}
			this.pos = this.parseTableRow(this.pos + 1);
			return true;
		} else {
			if (this.inTable) {
				this.sb.append("</tbody></table>\n");
				this.inTable = false;
			}
		}

		if (this.listLevel >= 0 || WikiParser.LIST_CHARS.indexOf(c) >= 0) { // lists
			int lc;
			// count list level
			for (lc = 0; lc <= this.listLevel && this.pos + lc < this.wikiLength
				&& this.wikiChars[this.pos + lc] == this.listLevels[lc]; lc++) {
				;
			}

			if (lc <= this.listLevel) { // end list block(s)
				do {
					this.sb.append(WikiParser.LIST_CLOSE[WikiParser.LIST_CHARS
						.indexOf(this.listLevels[this.listLevel--])]);
				} while (lc <= this.listLevel);
				// list(s) closed => retry from the same position
				this.blockquoteBR = true;
				return true;
			} else {
				if (this.pos + lc >= this.wikiLength) {
					return false;
				}
				char cc = this.wikiChars[this.pos + lc];
				int listType = WikiParser.LIST_CHARS.indexOf(cc);
				if (listType >= 0 && this.pos + lc + 1 < this.wikiLength
					&& this.wikiChars[this.pos + lc + 1] != cc
					&& this.listLevel < WikiParser.MAX_LIST_LEVELS) { // new list block
					this.sb.append(WikiParser.LIST_OPEN[listType]);
					this.listLevels[++this.listLevel] = cc;
					this.blockquoteBR = true;
					this.pos = this.parseListItem(this.pos + lc + 1);
					return true;
				} else if (this.listLevel >= 0) { // list item - same level
					if (this.listLevels[this.listLevel] == '>'
						|| this.listLevels[this.listLevel] == ':') {
						this.sb.append('\n');
					} else if (this.listLevels[this.listLevel] == '!') {
						this.sb.append("</div>\n<div class='center'>");
					} else {
						this.sb.append("</li>\n<li>");
					}
					this.pos = this.parseListItem(this.pos + lc);
					return true;
				}
			}
		}

		if (c == '=') { // heading
			int hc;
			// count heading level
			for (hc = 1; hc < 6 && this.pos + hc < this.wikiLength
				&& this.wikiChars[this.pos + hc] == '='; hc++) {
				;
			}
			if (this.pos + hc >= this.wikiLength) {
				return false;
			}
			int p;
			for (p = this.pos + hc; p < this.wikiLength
				&& (this.wikiChars[p] == ' ' || this.wikiChars[p] == '\t'); p++) {
				; // skip spaces
			}
			String tagName = "h" + (hc + this.HEADING_LEVEL_SHIFT);
			this.sb.append("<" + tagName + " id=''>"); // real id to be inserted after
																									// parsing this item
			int hStart = this.sb.length();
			this.pos = this.parseItem(p,
				this.wikiText.substring(this.pos, this.pos + hc), ContextType.HEADER);
			String hText = this.sb.substring(hStart, this.sb.length());
			this.sb.append("</" + tagName + ">\n");
			String anchorId = this.generateTOCAnchorId(hc, hText);
			this.sb.insert(hStart - 2, anchorId);
			this.appendTOCItem(hc, anchorId, hText);
			return true;
		} else if (c == '{') { // nowiki-block?
			if (this.pos + 2 < this.wikiLength && this.wikiChars[this.pos + 1] == '{'
				&& this.wikiChars[this.pos + 2] == '{') {
				int startNowiki = this.pos + 3;
				int endNowiki = this.findEndOfNowiki(startNowiki);
				int endPos = endNowiki + 3;
				if (this.wikiText.lastIndexOf('\n', endNowiki) >= startNowiki) { // block
																																					// <pre>
					if (this.wikiChars[startNowiki] == '\n') {
						startNowiki++; // skip the very first '\n'
					}
					if (this.wikiChars[endNowiki - 1] == '\n') {
						endNowiki--; // omit the very last '\n'
					}
					this.sb.append("<pre>");
					this.appendNowiki(this.wikiText.substring(startNowiki, endNowiki));
					this.sb.append("</pre>\n");
					this.pos = endPos;
					return true;
				}
				// else inline <nowiki> - proceed to regular paragraph handling
			} else if (this.pos + 1 < this.wikiLength
				&& this.wikiChars[this.pos + 1] == '|') { // mediawiki-table?
				int pp;
				for (pp = this.pos + 2; pp < this.wikiLength
					&& (this.wikiChars[pp] == ' ' || this.wikiChars[pp] == '\t'); pp++) {
					; // skip spaces
				}
				if (pp == this.wikiLength || this.wikiChars[pp] == '\n') { // yes, it's
																																		// start of
																																		// a table
					this.sb.append("<table border=\"1\"><tr><td>");
					this.mediawikiTableLevel++;
					this.pos = pp + 1;
					return pp < this.wikiLength;
				}
			}
		} else if (c == '-' && this.wikiText.startsWith("----", this.pos)) {
			int p;
			for (p = this.pos + 4; p < this.wikiLength
				&& (this.wikiChars[p] == ' ' || this.wikiChars[p] == '\t'); p++) {
				; // skip spaces
			}
			if (p == this.wikiLength || this.wikiChars[p] == '\n') {
				this.sb.append("\n<hr/>\n");
				this.pos = p;
				return true;
			}
		} else if (c == '~') { // block-level escaping: '*' '-' '#' '>' ':' '!' '|'
														// '='
			if (this.pos + 1 < this.wikiLength) {
				char nc = this.wikiChars[this.pos + 1];
				if (nc == '>' || nc == ':' || nc == '-' || nc == '|' || nc == '='
					|| nc == '!') { // can't be inline markup
					this.pos++; // skip '~' and proceed to regular paragraph handling
					c = nc;
				} else if (nc == '*' || nc == '#') { // might be inline markup so need
																							// to double check
					char nnc = this.pos + 2 < this.wikiLength ? this.wikiChars[this.pos + 2]
						: 0;
					if (nnc != nc) {
						this.pos++; // skip '~' and proceed to regular paragraph handling
						c = nc;
					}
					// otherwise escaping will be done at line level
				} else if (nc == '{') { // might be inline {{{ markup so need to double
																// check
					char nnc = this.pos + 2 < this.wikiLength ? this.wikiChars[this.pos + 2]
						: 0;
					if (nnc == '|') { // mediawiki-table?
						this.pos++; // skip '~' and proceed to regular paragraph handling
						c = nc;
					}
					// otherwise escaping will be done at line level
				}
			}
		}

		{ // paragraph handling
			this.sb.append("<p>");
			this.pos = this.parseItem(this.pos, null, ContextType.PARAGRAPH);
			this.sb.append("</p>\n");
			return true;
		}
	}

	/**
	 * Finds first closing '}}}' for nowiki block or span. Skips escaped
	 * sequences: '~}}}'.
	 * 
	 * @param startBlock
	 *          points to first char after '{{{'
	 * @return position of first '}' in closing '}}}'
	 */
	private int findEndOfNowiki(final int startBlock) {
		// NOTE: this method could step back one char from startBlock position
		int endBlock = startBlock - 3;
		do {
			endBlock = this.wikiText.indexOf("}}}", endBlock + 3);
			if (endBlock < 0) {
				return this.wikiLength; // no matching '}}}' found
			}
			while (endBlock + 3 < this.wikiLength
				&& this.wikiChars[endBlock + 3] == '}') {
				endBlock++; // shift to end of sequence of more than 3x'}' (eg. '}}}}}')
			}
		} while (this.wikiChars[endBlock - 1] == '~');
		return endBlock;
	}

	/**
	 * Greedy version of findEndOfNowiki(). It finds the last possible closing
	 * '}}}' before next opening '{{{'. Also uses escapes '~{{{' and '~}}}'.
	 * 
	 * @param startBlock
	 *          points to first char after '{{{'
	 * @return position of first '}' in closing '}}}'
	 */
	@SuppressWarnings("unused")
	private int findEndOfNowikiGreedy(final int startBlock) {
		// NOTE: this method could step back one char from startBlock position
		int nextBlock = startBlock - 3;
		do {
			do {
				nextBlock = this.wikiText.indexOf("{{{", nextBlock + 3);
			} while (nextBlock > 0 && this.wikiChars[nextBlock - 1] == '~');
			if (nextBlock < 0) {
				nextBlock = this.wikiLength;
			}
			int endBlock = this.wikiText.lastIndexOf("}}}", nextBlock);
			if (endBlock >= startBlock && this.wikiChars[endBlock - 1] != '~') {
				return endBlock;
			}
		} while (nextBlock < this.wikiLength);
		return this.wikiLength;
	}

	/**
	 * @param start
	 *          points to first char after pipe '|'
	 * @return
	 */
	private int parseTableRow(int start) {
		if (start >= this.wikiLength) {
			return this.wikiLength;
		}

		boolean endOfRow = false;
		boolean first = true;
		do {
			int colspan = 0;
			while (start + colspan < this.wikiLength
				&& this.wikiChars[start + colspan] == '|') {
				colspan++;
			}
			start += colspan;
			colspan++;
			boolean th = start < this.wikiLength && this.wikiChars[start] == '=';
			if (first) {
				if (!th && this.inHead) {
					this.sb.append("</thead><tbody><tr>");
					this.inHead = false;
				} else {
					this.sb.append("<tr>");
				}
				first = false;
			}
			start += (th ? 1 : 0);
			while (start < this.wikiLength && this.wikiChars[start] <= ' '
				&& this.wikiChars[start] != '\n') {
				start++; // trim whitespace from the start
			}

			if (start >= this.wikiLength || this.wikiChars[start] == '\n') { // skip
																																				// last
																																				// empty
																																				// column
				start++; // eat '\n'
				break;
			}

			this.sb.append(th ? "<th" : "<td");
			if (colspan > 1) {
				this.sb.append(" colspan=\"" + colspan + "\"");
			}
			this.sb.append('>');
			try {
				this.parseItemThrow(start, null, ContextType.TABLE_CELL);
			} catch (EndOfSubContextException e) { // end of cell
				start = e.position;
				if (start >= this.wikiLength) {
					endOfRow = true;
				} else if (this.wikiChars[start] == '\n') {
					start++; // eat '\n'
					endOfRow = true;
				}
			} catch (EndOfContextException e) {
				start = e.position;
				endOfRow = true;
			}
			this.sb.append(th ? "</th>" : "</td>");
		} while (!endOfRow/* && start<wikiLength && wikiChars[start]!='\n' */);
		this.sb.append("</tr>\n");
		return start;
	}

	/**
	 * Same as parseItem(); blank line adds &lt;br/&gt;&lt;br/&gt;
	 * 
	 * @param start
	 */
	private int parseListItem(int start) {
		while (start < this.wikiLength && this.wikiChars[start] <= ' '
			&& this.wikiChars[start] != '\n') {
			start++; // skip spaces
		}
		int end = this.parseItem(start, null, ContextType.LIST_ITEM);
		if ((this.listLevels[this.listLevel] == '>' || this.listLevels[this.listLevel] == ':')
			&& this.wikiText.substring(start, end).trim().length() == 0) { // empty
																																			// line
																																			// within
																																			// blockquote/div
			if (!this.blockquoteBR) {
				this.sb.append("<br/><br/>");
				this.blockquoteBR = true;
			}
		} else {
			this.blockquoteBR = false;
		}
		return end;
	}

	/**
	 * @param p
	 *          points to first slash in suspected URI (scheme://etc)
	 * @param start
	 *          points to beginning of parsed item
	 * @param end
	 *          points to end of parsed item
	 * 
	 * @return array of two integer offsets [begin_uri, end_uri] if matched, null
	 *         otherwise
	 */
	private int[] checkURI(final int p, final int start, final int end) {
		if (p > start && this.wikiChars[p - 1] == ':') { // "://" found
			int pb = p - 1;
			while (pb > start && Utils.isLatinLetterOrDigit(this.wikiChars[pb - 1])) {
				pb--;
			}
			int pe = p + 2;
			while (pe < end && Utils.isUrlChar(this.wikiChars[pe])) {
				pe++;
			}
			URI uri = null;
			do {
				while (pe > p + 2 && ",.;:?!%)".indexOf(this.wikiChars[pe - 1]) >= 0) {
					pe--; // don't want these chars at the end of URI
				}
				try { // verify URL syntax
					uri = new URI(this.wikiText.substring(pb, pe));
				} catch (URISyntaxException e) {
					pe--; // try chopping from the end
				}
			} while (uri == null && pe > p + 2);
			if (uri != null && uri.isAbsolute() && !uri.isOpaque()) {
				int offs[] = { pb, pe };
				return offs;
			}
		}
		return null;
	}

	private int parseItem(final int start, final String delimiter,
		final ContextType context) {
		try {
			return this.parseItemThrow(start, delimiter, context);
		} catch (EndOfContextException e) {
			return e.position;
		}
	}

	private int parseItemThrow(final int start, final String delimiter,
		final ContextType context) throws EndOfContextException {
		StringBuilder tb = new StringBuilder();

		boolean specialCaseDelimiterHandling = "//".equals(delimiter);
		int p = start;
		int end = this.wikiLength;

		try {
			nextChar: while (true) {
				if (p >= end) {
					throw new EndOfContextException(end); // break;
				}

				if (delimiter != null && this.wikiText.startsWith(delimiter, p)) {
					if (!specialCaseDelimiterHandling
						|| this.checkURI(p, start, end) == null) {
						p += delimiter.length();
						return p;
					}
				}

				char c = this.wikiChars[p];
				boolean atLineStart = false;

				// context-defined break test
				if (c == '\n') {
					if (context == ContextType.HEADER
						|| context == ContextType.TABLE_CELL) {
						p++;
						throw new EndOfContextException(p);
					}
					if (p + 1 < end && this.wikiChars[p + 1] == '\n') { // blank line
																															// delimits
																															// everything
						p++; // eat one '\n' and leave another one unparsed so parseBlock()
									// can close all lists
						throw new EndOfContextException(p);
					}
					for (p++; p < end && this.wikiChars[p] <= ' '
						&& this.wikiChars[p] != '\n'; p++) {
						; // skip whitespace
					}
					if (p >= end) {
						throw new EndOfContextException(p); // end of text reached
					}

					c = this.wikiChars[p];
					atLineStart = true;

					if (c == '-' && this.wikiText.startsWith("----", p)) { // check for
																																	// ---- <hr>
						int pp;
						for (pp = p + 4; pp < end
							&& (this.wikiChars[pp] == ' ' || this.wikiChars[pp] == '\t'); pp++) {
							; // skip spaces
						}
						if (pp == end || this.wikiChars[pp] == '\n') {
							throw new EndOfContextException(p); // yes, it's <hr>
						}
					}

					if (WikiParser.LIST_CHARS.indexOf(c) >= 0) { // start of list item?
						if (WikiParser.FORMAT_CHARS.indexOf(c) < 0) {
							throw new EndOfContextException(p);
						}
						// here we have a list char, which also happen to be a format char
						if (p + 1 < end && this.wikiChars[p + 1] != c) {
							throw new EndOfContextException(p); // format chars go in pairs
						}
						if (/* context==ContextType.LIST_ITEM */this.listLevel >= 0
							&& c == this.listLevels[0]) {
							// c matches current list's first level, so it must be new list
							// item
							throw new EndOfContextException(p);
						}
						// otherwise it must be just formatting sequence => no break of
						// context
					} else if (c == '=') { // header
						throw new EndOfContextException(p);
					} else if (c == '|') { // table or mediawiki-table
						throw new EndOfContextException(p);
					} else if (c == '{') { // mediawiki-table?
						if (p + 1 < end && this.wikiChars[p + 1] == '|') {
							int pp;
							for (pp = p + 2; pp < end
								&& (this.wikiChars[pp] == ' ' || this.wikiChars[pp] == '\t'); pp++) {
								; // skip spaces
							}
							if (pp == end || this.wikiChars[pp] == '\n') {
								throw new EndOfContextException(p); // yes, it's start of a
																										// table
							}
						}
					}

					// if none matched add '\n' to text buffer
					tb.append('\n');
					// p and c already shifted past the '\n' and whitespace after, so go
					// on
				} else if (c == '|') {
					if (context == ContextType.TABLE_CELL) {
						p++;
						throw new EndOfSubContextException(p);
					}
				}

				int formatType;

				if (c == '{') {
					if (p + 1 < end && this.wikiChars[p + 1] == '{') {
						if (p + 2 < end && this.wikiChars[p + 2] == '{') { // inline or
																																// block
																																// <nowiki>
							this.appendText(tb.toString());
							tb.delete(0, tb.length()); // flush text buffer
							int startNowiki = p + 3;
							int endNowiki = this.findEndOfNowiki(startNowiki);
							p = endNowiki + 3;
							if (this.wikiText.lastIndexOf('\n', endNowiki) >= startNowiki) { // block
																																								// <pre>
								if (this.wikiChars[startNowiki] == '\n') {
									startNowiki++; // skip the very first '\n'
								}
								if (this.wikiChars[endNowiki - 1] == '\n') {
									endNowiki--; // omit the very last '\n'
								}
								if (context == ContextType.PARAGRAPH) {
									this.sb.append("</p>"); // break the paragraph because XHTML
																					// does not allow <pre> children of
																					// <p>
								}
								this.sb.append("<pre>");
								this.appendNowiki(this.wikiText.substring(startNowiki,
									endNowiki));
								this.sb.append("</pre>\n");
								if (context == ContextType.PARAGRAPH) {
									this.sb.append("<p>"); // continue the paragraph
									// if (context==ContextType.NOWIKI_BLOCK) return p; // in this
									// context return immediately after nowiki
								}
							} else { // inline <nowiki>
								this.appendNowiki(this.wikiText.substring(startNowiki,
									endNowiki));
							}
							continue;
						} else if (p + 2 < end) { // {{image}}
							int endImg = this.wikiText.indexOf("}}", p + 2);
							if (endImg >= 0 && endImg < end) {
								this.appendText(tb.toString());
								tb.delete(0, tb.length()); // flush text buffer
								this.appendImage(this.wikiText.substring(p + 2, endImg));
								p = endImg + 2;
								continue;
							}
						}
					}
				} else if (c == '[') {
					if (p + 1 < end && this.wikiChars[p + 1] == '[') { // [[link]]
						int endLink = this.wikiText.indexOf("]]", p + 2);
						if (endLink >= 0 && endLink < end) {
							this.appendText(tb.toString());
							tb.delete(0, tb.length()); // flush text buffer
							this.appendLink(this.wikiText.substring(p + 2, endLink));
							p = endLink + 2;
							continue;
						}
					}
				} else if (c == '\\') {
					if (p + 1 < end && this.wikiChars[p + 1] == '\\') { // \\ = <br/>
						this.appendText(tb.toString());
						tb.delete(0, tb.length()); // flush text buffer
						this.sb.append("<br/>");
						p += 2;
						continue;
					}
				} else if (c == '<') {
					if (p + 1 < end && this.wikiChars[p + 1] == '<') {
						if (p + 2 < end && this.wikiChars[p + 2] == '<') { // <<<macro>>>
							int endMacro = this.wikiText.indexOf(">>>", p + 3);
							if (endMacro >= 0 && endMacro < end) {
								this.appendText(tb.toString());
								tb.delete(0, tb.length()); // flush text buffer
								this.appendMacro(this.wikiText.substring(p + 3, endMacro));
								p = endMacro + 3;
								continue;
							}
						}
					}
				} else if ((formatType = WikiParser.FORMAT_CHARS.indexOf(c)) >= 0) {
					if (p + 1 < end && this.wikiChars[p + 1] == c) {
						this.appendText(tb.toString());
						tb.delete(0, tb.length()); // flush text buffer
						if (c == '/') { // special case for "//" - check if it is part of
														// URL (scheme://etc)
							int[] uriOffs = this.checkURI(p, start, end);
							if (uriOffs != null) {
								int pb = uriOffs[0], pe = uriOffs[1];
								if (pb > start && this.wikiChars[pb - 1] == '~') {
									this.sb.delete(this.sb.length() - (p - pb + 1),
										this.sb.length()); // roll back URL + tilde
									this.sb.append(Utils.escapeHTML(this.wikiText.substring(pb,
										pe)));
								} else {
									this.sb.delete(this.sb.length() - (p - pb), this.sb.length()); // roll
																																									// back
																																									// URL
									this.appendLink(this.wikiText.substring(pb, pe));
								}
								p = pe;
								continue;
							}
						}
						this.sb.append(WikiParser.FORMAT_TAG_OPEN[formatType]);
						try {
							p = this.parseItemThrow(p + 2,
								WikiParser.FORMAT_DELIM[formatType], context);
						} finally {
							this.sb.append(WikiParser.FORMAT_TAG_CLOSE[formatType]);
						}
						continue;
					}
				} else if (c == '~') { // escape
					// most start line escapes are dealt with in parseBlock()
					if (atLineStart) {
						// same as block-level escaping: '*' '-' '#' '>' ':' '|' '='
						if (p + 1 < end) {
							char nc = this.wikiChars[p + 1];
							if (nc == '>' || nc == ':' || nc == '-' || nc == '|' || nc == '='
								|| nc == '!') { // can't be inline markup
								tb.append(nc);
								p += 2; // skip '~' and nc
								continue nextChar;
							} else if (nc == '*' || nc == '#') { // might be inline markup so
																										// need to double check
								char nnc = p + 2 < end ? this.wikiChars[p + 2] : 0;
								if (nnc != nc) {
									tb.append(nc);
									p += 2; // skip '~' and nc
									continue nextChar;
								}
								// otherwise escaping will be done at line level
							} else if (nc == '{') { // might be inline {{{ markup so need to
																			// double check
								char nnc = p + 2 < end ? this.wikiChars[p + 2] : 0;
								if (nnc == '|') { // mediawiki-table?
									tb.append(nc);
									tb.append(nnc);
									p += 3; // skip '~', nc and nnc
									continue nextChar;
								}
								// otherwise escaping will be done as usual at line level
							}
						}
					}
					for (String e : WikiParser.ESCAPED_INLINE_SEQUENCES) {
						if (this.wikiText.startsWith(e, p + 1)) {
							tb.append(e);
							p += 1 + e.length();
							continue nextChar;
						}
					}
				} else if (c == '-') { // ' -- ' => &ndash;
					if (p + 2 < end && this.wikiChars[p + 1] == '-'
						&& this.wikiChars[p + 2] == ' ' && p > start
						&& this.wikiChars[p - 1] == ' ') {
						// appendText(tb.toString()); tb.delete(0, tb.length()); // flush
						// text buffer
						// sb.append("&ndash; ");
						tb.append("&ndash; "); // &ndash; = "\u2013 "
						p += 3;
						continue;
					}
				}
				tb.append(c);
				p++;
			}
		} finally {
			this.appendText(tb.toString());
			tb.delete(0, tb.length()); // flush text buffer
		}
	}

	protected void appendMacro(final String text) {
		if ("TOC".equals(text)) {
			this.sb.append("<<<TOC>>>"); // put TOC placeholder for replacing it later
																		// with real TOC
		} else {
			this.sb.append("&lt;&lt;&lt;Macro:");
			this.sb.append(Utils.escapeHTML(Utils.unescapeHTML(text)));
			this.sb.append("&gt;&gt;&gt;");
		}
	}

	protected void appendLink(final String text) {
		String[] link = Utils.split(text, '|');
		URI uri = null;
		try { // validate URI
			uri = new URI(link[0].trim());
		} catch (URISyntaxException e) {
		}
		if (uri != null && uri.isAbsolute() && !uri.isOpaque()) {
			this.sb.append("<a href=\"" + Utils.escapeHTML(uri.toString())
				+ "\" rel=\"nofollow\">");
			this.sb.append(Utils.escapeHTML(Utils.unescapeHTML(link.length >= 2
				&& !Utils.isEmpty(link[1].trim()) ? link[1] : link[0])));
			this.sb.append("</a>");
		} else {
			this.sb.append("<a href=\"#\" title=\"Internal link\">");
			this.sb.append(Utils.escapeHTML(Utils.unescapeHTML(link.length >= 2
				&& !Utils.isEmpty(link[1].trim()) ? link[1] : link[0])));
			this.sb.append("</a>");
		}
	}

	protected void appendImage(final String text) {
		String[] link = Utils.split(text, '|');
		URI uri = null;
		try { // validate URI
			uri = new URI(link[0].trim());
		} catch (URISyntaxException e) {
		}
		if (uri != null && uri.isAbsolute() && !uri.isOpaque()) {
			String alt = Utils.escapeHTML(Utils.unescapeHTML(link.length >= 2
				&& !Utils.isEmpty(link[1].trim()) ? link[1] : link[0]));
			this.sb.append("<img src=\"" + Utils.escapeHTML(uri.toString())
				+ "\" alt=\"" + alt + "\" title=\"" + alt + "\" />");
		} else {
			this.sb.append("&lt;&lt;&lt;Internal image(?): ");
			this.sb.append(Utils.escapeHTML(Utils.unescapeHTML(text)));
			this.sb.append("&gt;&gt;&gt;");
		}
	}

	protected void appendText(final String text) {
		this.sb.append(Utils.escapeHTML(Utils.unescapeHTML(text)));
	}

	protected String generateTOCAnchorId(final int hLevel, final String text) {
		int i = 0;
		String id = (this.HEADING_ID_PREFIX != null ? this.HEADING_ID_PREFIX : "H"
			+ hLevel + "_")
			+ Utils.translit(text.replaceAll("<.+?>", "")).trim()
				.replaceAll("\\s+", "_").replaceAll("[^a-zA-Z0-9_-]", "");
		while (this.tocAnchorIds.contains(id)) { // avoid duplicates
			i++;
			id = text + "_" + i;
		}
		this.tocAnchorIds.add(id);
		return id;
	}

	protected void appendTOCItem(final int level, final String anchorId,
		final String text) {
		if (level > this.tocLevel) {
			while (level > this.tocLevel) {
				this.toc.append("<ul><li>");
				this.tocLevel++;
			}
		} else {
			while (level < this.tocLevel) {
				this.toc.append("</li></ul>");
				this.tocLevel--;
			}
			this.toc.append("</li>\n<li>");
		}
		this.toc.append("<a href='#" + anchorId + "'>" + text + "</a>");
	}

	protected void completeTOC() {
		while (0 < this.tocLevel) {
			this.toc.append("</li></ul>");
			this.tocLevel--;
		}
		int idx;
		String tocDiv = "<div class='toc'>" + this.toc.toString() + "</div>";
		while ((idx = this.sb.indexOf("<<<TOC>>>")) >= 0) {
			this.sb.replace(idx, idx + 9, tocDiv);
		}
	}

	protected void appendNowiki(final String text) {
		this.sb.append(Utils.escapeHTML(Utils.replaceString(
			Utils.replaceString(text, "~{{{", "{{{"), "~}}}", "}}}")));
	}

	private static class EndOfContextException extends Exception {
		private static final long serialVersionUID = 1L;
		int position;

		public EndOfContextException(final int position) {
			super();
			this.position = position;
		}
	}

	private static class EndOfSubContextException extends EndOfContextException {
		private static final long serialVersionUID = 1L;

		public EndOfSubContextException(final int position) {
			super(position);
		}
	}
}
