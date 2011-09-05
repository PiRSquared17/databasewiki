syntax clear

syn match wikiLine "^----$"
syn match wikiList "^[*#]* "
syn region wikiLink start=+\[\[+ end=+\]\]+
syn region wikiLink2 start=+{\[+ end=+\]}+
syn region wikiImage start=+{{+ end=+}}+
syn region wikiCurly start=+{{{+ end=+}}}+
syn region wikiHead start="^=" end="$"
syn region wikiTable start="^|" end="|$"
syn region wikiSQL start="{sql" end="{/sql}" contains=wikiQuerySQL,wikiBodySQL
syn region wikiQuerySQL start=+{query}+ms=e+1 end=+{/query}+me=s-1 contained
syn region wikiBodySQL start=+{body}+ms=e+1 end=+{/body}+me=s-1 contained contains=ALL
syn region wikiNew start="{new" end="{/new}"
syn region wikiMath start="{math" end="{/math}"
syn region wikiFMStart start=+\[#+ end=+\]+
syn region wikiFMEnd start=+\[/#+ end=+\]+

hi def wikiHead term=underline ctermfg=Red guifg=Red
hi def wikiLine ctermfg=DarkRed guifg=DarkRed
hi def wikiList ctermfg=Yellow guifg=Yellow
hi def wikiLink ctermfg=Green guifg=Green
hi def wikiLink2 ctermfg=Green guifg=Green
hi def wikiImage ctermfg=DarkGreen guifg=DarkGreen
hi def wikiTable ctermfg=Cyan guifg=Cyan
hi def wikiSQL ctermfg=Magenta guifg=Magenta
hi def wikiQuerySQL ctermfg=Cyan guifg=Cyan
hi def wikiCurly ctermfg=Blue guifg=Blue
hi def wikiMath ctermfg=DarkMagenta guifg=DarkMagenta
hi def wikiNew ctermfg=DarkMagenta guifg=DarkMagenta
hi def wikiFMStart ctermfg=DarkBlue guifg=DarkBlue
hi def wikiFMEnd ctermfg=DarkBlue guifg=DarkBlue

let b:current_syntax = "dbw"
