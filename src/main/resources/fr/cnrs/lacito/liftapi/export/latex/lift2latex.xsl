<?xml version="1.0" encoding="utf-8"?>
<!--
 | LIFT (Lexicon Interchange FormaT) to LaTeX.
 |
 | XSLT 1.0, so that it runs with the JDK's processor (javax.xml.transform) and with any
 | other XSLT 1.0 processor (xsltproc). The parameters are in parameters.xsl; the LaTeX
 | preamble and the commands that style the dictionary are in latex.xsl.
 |
 | The output needs XeLaTeX or LuaLaTeX (fontspec, polyglossia).
 |
 | Customising: a custom stylesheet, loaded after these ones, can replace the definitions
 | below (lift-api latex: option stylesheet; JSON: customStylesheet). Its definitions win
 | over these ones, and it can call any template of these files. "lift-api latex" with the
 | option write-stylesheet writes a starting file. These are kept stable; the rest
 | (escape_for_latex, label, letter, in-language, the keys, the folding strings) is
 | internal and may change.
 |
 |   latex-preamble, latex-package       the document class, the packages, the layout
 |   latex-styling-command                the macros that style the entries: \entry,
 |                                        \latexsensestart, \glossfr, \definition...
 |   latex-startdocument, latex-enddocument, latex-startsection, latex-endsection
 |                                        the title page, the contents, the sections
 |   match="entry", "sense", "gloss"      an entry, a sense, a gloss
 |   make_headword, make_headword_string  a headword (param entry)
 |   make_illustration                    the figures of a sense (param sense)
 |   abbreviate_pos, abbreviate_trait, abbreviate_slot
 |                                        the abbreviations (params pos, trait, slot)
 |   the xsl:param of parameters.xsl      their defaults
 -->
<xsl:stylesheet
	version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:import href="parameters.xsl"/>
	<xsl:import href="latex.xsl"/>

	<xsl:output encoding="UTF-8" method="text"/>

<!--
............................................................
! Keys
............................................................
-->
	<!--
	 | The letter of an entry: the first character of its form, after removing U+034F
	 | (combining grapheme joiner) and skipping a leading "-", without case or accent.
	 | XSLT 2.0 grouped with xsl:for-each-group group-by="if (starts-with(form, '-'))
	 | then substring(form, 2, 1) else substring(form, 1, 1)".
	 |
	 | An xsl:key cannot refer to a variable: the two strings of the outer translate() are
	 | $foldFrom and $foldTo of parameters.xsl, which must stay the same. The template
	 | "letter" computes the same value for the lookups. Not handled: invisible characters
	 | other than U+034F at the start of a form.
	 -->
	<xsl:key name="letter" match="entry"
		use="translate(substring(translate(string(lexical-unit/form[1]/text), '&#x34F;', ''), 1 + starts-with(translate(string(lexical-unit/form[1]/text), '&#x34F;', ''), '-'), 1), 'ABCDEFGHIJKLMNOPQRSTUVWXYZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞàáâãäåçèéêëìíîïñòóôõöùúûüýÿĀāĂăĄąĆćĈĉĊċČčĎďĐĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦĨĩĪīĬĭĮįİĲĴĵĶķĹĺĻļĽľĿŁŃńŅņŇňŊŌōŎŏŐőŒŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſ', 'abcdefghijklmnopqrstuvwxyzaaaaaaæceeeeiiiiðnoooooøuuuuyþaaaaaaceeeeiiiinooooouuuuyyaaaaaaccccccccddđeeeeeeeeeegggggggghhħiiiiiiiiiĳjjkkllllllŀłnnnnnnŋooooooœrrrrrrssssssssttttŧuuuuuuuuuuuuwwyyyzzzzzzs')"/>

	<!-- The senses by semantic domain. XSLT 2.0: for-each-group group-by="@value". -->
	<xsl:key name="domain" match="sense/trait[@name='semantic-domain-ddp4']" use="@value"/>

	<!-- The relations of a sense by type. XSLT 2.0: for-each-group group-by="@type", within a sense. -->
	<xsl:key name="sense-relation" match="sense/relation[@type]" use="concat(generate-id(..), '|', @type)"/>

	<xsl:key name="entry" match="entry" use="@id"/>
	<xsl:key name="sense" match="sense" use="@id"/>

<!--
............................................................
! Global values
............................................................
-->
	<!-- The language of the sorts: never empty, so that the order does not depend on the machine. -->
	<xsl:variable name="sortLang">
		<xsl:choose>
			<xsl:when test="normalize-space($sortLanguage) != ''"><xsl:value-of select="normalize-space($sortLanguage)"/></xsl:when>
			<xsl:otherwise>en</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>

	<!--
	 | The parameters are passed explicitly: XSLTC orders the initialization of the global
	 | variables by the references in their own content, and would otherwise compute this
	 | one before the parameters are set.
	 -->
	<xsl:variable name="defaultPolyglossia">
		<xsl:call-template name="default-language">
			<xsl:with-param name="code" select="$sortLanguage"/>
			<xsl:with-param name="pairs" select="$languages"/>
		</xsl:call-template>
	</xsl:variable>

	<xsl:variable name="figures">
		<xsl:call-template name="is-true">
			<xsl:with-param name="value" select="$makeFigure"/>
		</xsl:call-template>
	</xsl:variable>

	<xsl:variable name="thematic">
		<xsl:call-template name="is-true">
			<xsl:with-param name="value" select="$makeThematicDictionary"/>
		</xsl:call-template>
	</xsl:variable>

	<!-- The characters LaTeX treats specially, escaped by "escape_for_latex". -->
	<xsl:variable name="latexSpecials" select="'\{}$&amp;#^_%~'"/>

	<!-- The characters kept in a \label; any other becomes "-". -->
	<xsl:variable name="labelChars" select="'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-:'"/>
	<xsl:variable name="dashes" select="'--------------------------------------------------------------------------------------------------------------------------------'"/>

<!--
............................................................
! The document
............................................................
-->
	<xsl:template match="/">
	<xsl:call-template name="latex-preamble" />
	<xsl:call-template name="latex-startdocument" />
	<xsl:text>
	\section{Alphabetic dictionary}
	</xsl:text>

	<!-- One entry per letter: the first of its group (Muenchian grouping). -->
	<xsl:for-each select="/lift/entry[generate-id() = generate-id(key('letter', translate(substring(translate(string(lexical-unit/form[1]/text), '&#x34F;', ''), 1 + starts-with(translate(string(lexical-unit/form[1]/text), '&#x34F;', ''), '-'), 1), $foldFrom, $foldTo))[1])]">
	<xsl:sort select="translate(substring(translate(string(lexical-unit/form[1]/text), '&#x34F;', ''), 1 + starts-with(translate(string(lexical-unit/form[1]/text), '&#x34F;', ''), '-'), 1), $foldFrom, $foldTo)" lang="{$sortLang}"/>
	<xsl:variable name="letter">
		<xsl:call-template name="letter"/>
	</xsl:variable>
	<!-- An entry with no form has no letter: it is left out of this part. -->
	<xsl:if test="$letter != ''">
	<xsl:call-template name="latex-startsection">
	<xsl:with-param name="letter" select="translate($letter, $upperFrom, $upperTo)" />
	</xsl:call-template>
	<xsl:for-each select="key('letter', $letter)">
	<xsl:sort select="substring(translate(string(lexical-unit/form[1]/text), '&#x34F;', ''), 1 + starts-with(translate(string(lexical-unit/form[1]/text), '&#x34F;', ''), '-'))" lang="{$sortLang}"/>
	<xsl:text>\label{</xsl:text>
	<xsl:call-template name="label">
		<xsl:with-param name="string" select="@id"/>
	</xsl:call-template>
	<xsl:text>}</xsl:text>
	<xsl:apply-templates select="."/>
	</xsl:for-each>
	<xsl:call-template name="latex-endsection"/>

	<xsl:if test="$figures = 'true'">
	<xsl:for-each select="key('letter', $letter)//sense[illustration]">
	<xsl:call-template name="make_illustration">
	<xsl:with-param name="sense" select="." />
	</xsl:call-template>
	</xsl:for-each>
	</xsl:if>
	</xsl:if>
	</xsl:for-each>

	<xsl:if test="$thematic = 'true'">
	<xsl:text>
	\section{Thematic dictionary}
	</xsl:text>
	<xsl:for-each select="/lift/entry//sense/trait[@name='semantic-domain-ddp4'][generate-id() = generate-id(key('domain', @value)[1])]">
	<!--
	 | Domains are numbered ("5 La maison", "5.1.1 Mobilier"): sort on the numbers, up to
	 | four levels, so that "5" comes before "5.1", then on the text.
	 -->
	<xsl:sort select="substring-before(concat(substring-before(concat(@value, ' '), ' '), '.'), '.')" data-type="number"/>
	<xsl:sort select="substring-before(concat(substring-after(substring-before(concat(@value, ' '), ' '), '.'), '.'), '.')" data-type="number"/>
	<xsl:sort select="substring-before(concat(substring-after(substring-after(substring-before(concat(@value, ' '), ' '), '.'), '.'), '.'), '.')" data-type="number"/>
	<xsl:sort select="substring-before(concat(substring-after(substring-after(substring-after(substring-before(concat(@value, ' '), ' '), '.'), '.'), '.'), '.'), '.')" data-type="number"/>
	<xsl:sort select="@value" lang="{$sortLang}"/>
	<xsl:call-template name="latex-startsection">
	<xsl:with-param name="letter" select="@value" />
	</xsl:call-template>
	<xsl:for-each select="key('domain', @value)">
	<xsl:sort select="substring(translate(string(ancestor::entry/lexical-unit/form[1]/text), '&#x34F;', ''), 1 + starts-with(translate(string(ancestor::entry/lexical-unit/form[1]/text), '&#x34F;', ''), '-'))" lang="{$sortLang}"/>
	<xsl:apply-templates select="ancestor::entry" />
	</xsl:for-each>
	<xsl:call-template name="latex-endsection"/>
	</xsl:for-each>
	</xsl:if>

	<xsl:call-template name="latex-enddocument" />
	</xsl:template>

	<!-- The letter of the current entry (see the key "letter"). -->
	<xsl:template name="letter">
	<xsl:variable name="form" select="translate(string(lexical-unit/form[1]/text), '&#x34F;', '')"/>
	<xsl:value-of select="translate(substring($form, 1 + starts-with($form, '-'), 1), $foldFrom, $foldTo)"/>
	</xsl:template>

<!--
............................................................
! Entry
............................................................
-->
	<xsl:template match="entry">
	<xsl:text>\latexentrystart</xsl:text>
	<xsl:call-template name="make_headword">
	<xsl:with-param name="entry" select="." />
	</xsl:call-template>

	<xsl:if test="variant">
	<xsl:text>\latexvariantstart</xsl:text>
	<xsl:for-each select="variant">
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'variant'" />
	<xsl:with-param name="content" select="string(form[1]/text)" />
	<xsl:with-param name="lang" select="form[1]/@lang" />
	</xsl:call-template>
	<xsl:if test="position() != last()">
	<xsl:text>\latexvariantbetween</xsl:text>
	</xsl:if>
	</xsl:for-each>
	<xsl:text>\latexvariantend</xsl:text>
	</xsl:if>

	<xsl:apply-templates select="sense" />

	<xsl:if test="relation[@type='_component-lexeme' and @ref]">
	<xsl:text>\latexrelationcomponentstart</xsl:text>
	<xsl:for-each select="relation[@type='_component-lexeme']">
	<xsl:if test="./trait[@name = 'complex-form-type']">
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'latexrelationcomponenttype'" />
<!--	il peut y en avoir d'autres ??? -->
	<xsl:with-param name="content" select="./trait[@name ='complex-form-type'][1]/@value"/>
	</xsl:call-template>
	</xsl:if>
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'latexrelationcomponent'" />
	<xsl:with-param name="escaped" select="true()" />
	<xsl:with-param name="content">
	<xsl:call-template name="make_headword_string">
	<xsl:with-param name="entry" select="key('entry', @ref)" />
	</xsl:call-template>
	</xsl:with-param>
	</xsl:call-template>
	<xsl:if test="position() != last()">
	<xsl:text>\latexrelationcomponentbetween</xsl:text>
	</xsl:if>
	</xsl:for-each>
	<xsl:text>\latexrelationcomponentend</xsl:text>
	</xsl:if>

	<xsl:text>\latexentryend</xsl:text>
	<xsl:text>&#10;</xsl:text>
	</xsl:template>

<!--
............................................................
! Sense
............................................................
-->
	<xsl:template match="sense">
	<xsl:text>\latexsensestart</xsl:text>

	<xsl:if test="preceding-sibling::sense or following-sibling::sense">
	<xsl:call-template name="make_sense_number">
	<xsl:with-param name="sense" select="."/>
	</xsl:call-template>
	</xsl:if>

	<xsl:if test="./grammatical-info/@value">
	<xsl:call-template name="make_sense_gram">
	<xsl:with-param name="sense" select="." />
	</xsl:call-template>
	</xsl:if>

	<xsl:if test="definition">
	<xsl:text>\latexsensedefinitionstart</xsl:text>
	<xsl:for-each select="definition">
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'definition'" />
	<xsl:with-param name="content" select="string(form[1]/text)" />
	<xsl:with-param name="lang" select="form[1]/@lang" />
	</xsl:call-template>
	<xsl:if test="position() != last()">
	<xsl:text>\latexsensedefinitionbetween</xsl:text>
	</xsl:if>
	</xsl:for-each>
	<xsl:text>\latexsensedefinitionend</xsl:text>
	</xsl:if>

	<xsl:if test="./gloss">
	<xsl:text>\latexsenseglossstart</xsl:text>
	<xsl:apply-templates select="gloss" />
	<xsl:text>\latexsenseglossend</xsl:text>
	</xsl:if>

	<xsl:if test="relation[@type]">
	<xsl:text>\latexsenserelationstart</xsl:text>
	<!-- One relation per type: the first of its group in this sense. -->
	<xsl:for-each select="relation[@type][generate-id() = generate-id(key('sense-relation', concat(generate-id(..), '|', @type))[1])]">
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'latexsenserelationtype'" />
	<xsl:with-param name="content" select="@type" />
	</xsl:call-template>
	<xsl:for-each select="key('sense-relation', concat(generate-id(..), '|', @type))">
	<xsl:variable name="target" select="key('sense', @ref)"/>
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'latexsenserelation'" />
	<xsl:with-param name="escaped" select="true()" />
	<xsl:with-param name="content">
	<xsl:call-template name="make_headword_string">
	<xsl:with-param name="entry" select="$target/ancestor::entry" />
	</xsl:call-template>
	</xsl:with-param>
	</xsl:call-template>
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'latexsenserelationsensenumber'" />
	<xsl:with-param name="content">
	<xsl:call-template name="make_sense_number_string">
	<xsl:with-param name="sense" select="$target" />
	</xsl:call-template>
	</xsl:with-param>
	</xsl:call-template>
	</xsl:for-each>
	<xsl:if test="position() != last()">
	<xsl:text>\latexsenserelationbetween</xsl:text>
	</xsl:if>
	</xsl:for-each>
	<xsl:text>\latexsenserelationend</xsl:text>
	</xsl:if>

	<xsl:if test="trait[@name='semantic-domain-ddp4']">
	<xsl:text>\latexsensedomainstart</xsl:text>
	<xsl:for-each select="trait[@name='semantic-domain-ddp4']">
	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'semanticdomain'" />
	<xsl:with-param name="content" select="./@value" />
	</xsl:call-template>
	<xsl:if test="position() != last()">
	<xsl:text>\latexsensedomainbetween</xsl:text>
	</xsl:if>
	</xsl:for-each>
	<xsl:text>\latexsensedomainend</xsl:text>
	</xsl:if>

	<xsl:if test="illustration and $figures = 'true'">
	<xsl:text>\ $\bullet$\ See figure </xsl:text>
	<xsl:variable name="sense" select="."/>
	<xsl:for-each select="illustration">
		<xsl:variable name="label">
			<xsl:call-template name="illustration-label">
				<xsl:with-param name="sense" select="$sense"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:text> \ref{</xsl:text>
		<xsl:value-of select="$label" />
		<xsl:text>}</xsl:text>
		<xsl:text> p. \pageref{</xsl:text>
		<xsl:value-of select="$label" />
		<xsl:text>}</xsl:text>
	<xsl:if test="position() != last()">
	<xsl:text>, </xsl:text>
	</xsl:if>
	</xsl:for-each>
	</xsl:if>

	<xsl:text>\latexsenseend</xsl:text>
	</xsl:template>

<!--
............................................................
! Gloss
............................................................
-->
	<xsl:template match="gloss">
	<!--
	 | A gloss made only of capitals, "-", "_" and "." is grammatical (COMPL): it is
	 | written in small capitals. XSLT 2.0: matches(text, '^[-_A-Z.]+$').
	 -->
	<xsl:variable name="text" select="string(text)"/>
	<xsl:choose>
	<xsl:when test="$text != '' and translate($text, '-_ABCDEFGHIJKLMNOPQRSTUVWXYZ.', '') = ''">
		<xsl:call-template name="make_command_with_string">
		<xsl:with-param name="command" select="'glossgramin'" />
		<xsl:with-param name="argument" select="@lang" />
		<xsl:with-param name="content" select="translate($text, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')" />
		<xsl:with-param name="lang" select="@lang" />
		</xsl:call-template>
	</xsl:when>
	<xsl:otherwise>
		<xsl:call-template name="make_command_with_string">
		<xsl:with-param name="command" select="'glossin'" />
		<xsl:with-param name="argument" select="@lang" />
		<xsl:with-param name="content" select="$text" />
		<xsl:with-param name="lang" select="@lang" />
		</xsl:call-template>
	</xsl:otherwise>
	</xsl:choose>
	</xsl:template>

<!--
............................................................
! Sense numbers
............................................................
-->
	<xsl:template name="make_sense_number">
	<xsl:param name="sense" />
	<xsl:if test="$sense/parent::sense">
	<xsl:call-template name="make_sense_number">
	<xsl:with-param name="sense" select="$sense/parent::sense"/>
	</xsl:call-template>
	<xsl:text>.</xsl:text>
	</xsl:if>
	<xsl:value-of select="concat('\ding{', 181 + 1 + count($sense/preceding-sibling::sense), '}')"/>
	</xsl:template>

	<xsl:template name="make_sense_number_string">
	<xsl:param name="sense" />
	<xsl:if test="$sense/parent::sense">
	<xsl:call-template name="make_sense_number_string">
	<xsl:with-param name="sense" select="$sense/parent::sense"/>
	</xsl:call-template>
	<xsl:text>.</xsl:text>
	</xsl:if>
	<xsl:if test="$sense">
	<xsl:value-of select="1 + count($sense/preceding-sibling::sense)"/>
	</xsl:if>
	</xsl:template>

<!--
............................................................
! Grammatical info
............................................................
 | <grammatical-info value="Noun">
 | <trait name="type" value="inflAffix"/>
 | <trait name="Noun-slot" value="NounGenderNumber"/></grammatical-info>
-->
	<xsl:template name="make_sense_gram">
	<xsl:param name="sense" />

		<xsl:text>\latexsensegramstart </xsl:text>

	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'pos'" />
	<xsl:with-param name="content">
	<xsl:call-template name="abbreviate_pos">
	<xsl:with-param name="pos" select="$sense/grammatical-info/@value" />
	</xsl:call-template>
	</xsl:with-param>
	</xsl:call-template>

	<xsl:for-each select="$sense/grammatical-info/trait[not(@name='type')]">

	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'gramtraitslot'" />
	<xsl:with-param name="content">
	<xsl:call-template name="abbreviate_slot">
	<xsl:with-param name="slot" select="./@name" />
	</xsl:call-template>
	</xsl:with-param>
	</xsl:call-template>

	<xsl:call-template name="make_command_with_string">
	<xsl:with-param name="command" select="'gramtrait'" />
	<xsl:with-param name="content">
	<xsl:call-template name="abbreviate_trait">
	<xsl:with-param name="trait" select="./@value" />
	</xsl:call-template>
	</xsl:with-param>
	</xsl:call-template>
	<xsl:if test="position() != last()">
	<xsl:text>, </xsl:text>
	</xsl:if>
	</xsl:for-each>

		<xsl:text>\latexsensegramend </xsl:text>
	</xsl:template>

<!--
............................................................
! Illustrations
............................................................
-->
	<xsl:template name="make_illustration">
	<xsl:param name="sense" />
	<xsl:for-each select="$sense/illustration">
	<xsl:text>\begin{figure}[tbp]
	\centering
	\includegraphics[width=0.3\linewidth]{</xsl:text>
	<xsl:value-of select="concat($urlillustration, @href)" />
	<xsl:text>}
	\caption{</xsl:text>
	<xsl:call-template name="make_headword_string">
	<xsl:with-param name="entry" select="ancestor::entry" />
	</xsl:call-template>
	<xsl:text>, see p. \pageref{</xsl:text>
	<xsl:call-template name="label">
		<xsl:with-param name="string" select="ancestor::entry/@id"/>
	</xsl:call-template>
	<xsl:text>}}
	\label{</xsl:text>
	<xsl:call-template name="illustration-label">
		<xsl:with-param name="sense" select="$sense"/>
	</xsl:call-template>
	<xsl:text>}
\end{figure}
</xsl:text>
	</xsl:for-each>
	</xsl:template>

	<!--
	 | The label of the current illustration of a sense: from the sense's id (or its
	 | entry's, for a sense without id) and the illustration's position in the sense.
	 -->
	<xsl:template name="illustration-label">
	<xsl:param name="sense"/>
	<xsl:text>illustration:</xsl:text>
	<xsl:call-template name="label">
		<xsl:with-param name="string">
			<xsl:choose>
				<xsl:when test="$sense/@id"><xsl:value-of select="$sense/@id"/></xsl:when>
				<xsl:otherwise><xsl:value-of select="concat($sense/ancestor::entry/@id, '-', count($sense/preceding::sense))"/></xsl:otherwise>
			</xsl:choose>
		</xsl:with-param>
	</xsl:call-template>
	<xsl:value-of select="concat('-', count(preceding-sibling::illustration) + 1)"/>
	</xsl:template>

<!--
............................................................
! Headwords
............................................................
-->
	<xsl:template name="make_headword">
	<xsl:param name="entry" />
	<xsl:text>\entry{</xsl:text>
	<xsl:call-template name="make_headword_string">
	<xsl:with-param name="entry" select="$entry" />
	</xsl:call-template>
	<xsl:text>}</xsl:text>
	</xsl:template>

	<!-- The form of an entry, escaped, in its language, with its homograph number. -->
	<xsl:template name="make_headword_string">
	<xsl:param name="entry" />
	<xsl:call-template name="in-language">
		<xsl:with-param name="text" select="string($entry/lexical-unit/form[1]/text)"/>
		<xsl:with-param name="lang" select="$entry/lexical-unit/form[1]/@lang"/>
	</xsl:call-template>
	<xsl:if test="$entry/@order">
		<xsl:value-of select="concat('$_{', $entry/@order, '}$')"/>
	</xsl:if>
	</xsl:template>

<!--
............................................................
! Writing LaTeX
............................................................
-->
	<!--
	 | \command{content}, or \command{argument}{content} when an argument is given.
	 | The content is escaped, unless "escaped" is true, and written in its language when
	 | "lang" is given.
	 -->
	<xsl:template name="make_command_with_string">
	<xsl:param name="command" />
	<xsl:param name="argument" select="''" />
	<xsl:param name="content" />
	<xsl:param name="lang" select="''" />
	<xsl:param name="escaped" select="false()" />
	<xsl:text>\</xsl:text>
	<xsl:value-of select="$command" />
	<xsl:if test="string($argument) != ''">
		<xsl:text>{</xsl:text>
		<xsl:value-of select="$argument" />
		<xsl:text>}</xsl:text>
	</xsl:if>
	<xsl:text>{</xsl:text>
	<xsl:choose>
	<xsl:when test="$escaped">
		<xsl:value-of select="$content"/>
	</xsl:when>
	<xsl:otherwise>
		<xsl:call-template name="in-language">
			<xsl:with-param name="text" select="string($content)"/>
			<xsl:with-param name="lang" select="string($lang)"/>
		</xsl:call-template>
	</xsl:otherwise>
	</xsl:choose>
	<xsl:text>}</xsl:text>
	</xsl:template>

	<!--
	 | A text, escaped, inside \foreignlanguage{name}{...} when its language has a
	 | polyglossia name other than the default language's.
	 -->
	<xsl:template name="in-language">
	<xsl:param name="text"/>
	<xsl:param name="lang" select="''"/>
	<xsl:variable name="name">
		<xsl:call-template name="polyglossia-name">
			<xsl:with-param name="code" select="string($lang)"/>
		</xsl:call-template>
	</xsl:variable>
	<!-- string(): comparing two result tree fragments directly is unreliable in XSLTC. -->
	<xsl:variable name="foreign" select="string($name) != '' and string($name) != string($defaultPolyglossia)"/>
	<xsl:if test="$foreign">
		<xsl:value-of select="concat('\foreignlanguage{', $name, '}{')"/>
	</xsl:if>
	<xsl:call-template name="escape_for_latex">
		<xsl:with-param name="string" select="$text"/>
	</xsl:call-template>
	<xsl:if test="$foreign">
		<xsl:text>}</xsl:text>
	</xsl:if>
	</xsl:template>

	<!--
	 | Escapes the characters LaTeX treats specially: \ { } $ & # ^ _ % ~. XSLT 2.0 used
	 | replace(), for \ and _ only. Each call writes the text up to the first special
	 | character, that character escaped, and recurses on the rest: the depth is the
	 | number of special characters, not the length of the text.
	 -->
	<xsl:template name="escape_for_latex">
	<xsl:param name="string" />
	<xsl:variable name="s" select="string($string)"/>
	<xsl:choose>
	<xsl:when test="translate($s, $latexSpecials, '') = $s">
		<xsl:value-of select="$s"/>
	</xsl:when>
	<xsl:otherwise>
		<!-- Every special character becomes "\": the text before the first one is the prefix. -->
		<xsl:variable name="before" select="substring-before(translate($s, $latexSpecials, '\\\\\\\\\\'), '\')"/>
		<xsl:variable name="c" select="substring($s, string-length($before) + 1, 1)"/>
		<xsl:value-of select="$before"/>
		<xsl:choose>
			<xsl:when test="$c = '\'">\textbackslash{}</xsl:when>
			<xsl:when test="$c = '^'">\^{}</xsl:when>
			<xsl:when test="$c = '~'">\textasciitilde{}</xsl:when>
			<xsl:otherwise><xsl:value-of select="concat('\', $c)"/></xsl:otherwise>
		</xsl:choose>
		<xsl:call-template name="escape_for_latex">
			<xsl:with-param name="string" select="substring($s, string-length($before) + 2)"/>
		</xsl:call-template>
	</xsl:otherwise>
	</xsl:choose>
	</xsl:template>

	<!--
	 | A string usable in \label and \ref: letters, digits, "-" and ":" are kept, any other
	 | character becomes "-". The ids of LIFT end with a guid, which keeps them unique.
	 -->
	<xsl:template name="label">
	<xsl:param name="string"/>
	<xsl:variable name="s" select="string($string)"/>
	<xsl:variable name="others" select="translate($s, $labelChars, '')"/>
	<xsl:value-of select="translate($s, $others, substring($dashes, 1, string-length($others)))"/>
	</xsl:template>

	<!-- 'true' when a boolean parameter is true: true(), or the string 'true'. -->
	<xsl:template name="is-true">
	<xsl:param name="value"/>
	<xsl:if test="$value = 'true'">true</xsl:if>
	</xsl:template>

<!--
............................................................
! Abbreviations
............................................................
-->
	<xsl:template name="abbreviate_pos">
	<xsl:param name="pos" />
	<xsl:choose>
	<xsl:when test="$pos='Noun'">n</xsl:when>
	<xsl:when test="$pos='Verb'">v</xsl:when>
	<xsl:when test="$pos='Adjective'">adj</xsl:when>
	<xsl:when test="$pos='Adverb'">adv</xsl:when>
	<xsl:when test="$pos='Possessive Pronoun'">poss</xsl:when>
	<xsl:when test="$pos='Cardinal number'">card.</xsl:when>
	<xsl:when test="$pos='n_Npr'">Npr</xsl:when>
	<xsl:when test="$pos='Personal pronoun'">pers. pro.</xsl:when>
	<xsl:when test="$pos='Interrogative pro-form'">inter. pro.</xsl:when>
	<xsl:when test="$pos='Pronoun'">pro.</xsl:when>
	<xsl:when test="$pos='Interjection'">interj.</xsl:when>
	<xsl:otherwise><xsl:value-of select="$pos"/></xsl:otherwise>
	</xsl:choose>
	</xsl:template>

	<xsl:template name="abbreviate_trait">
	<xsl:param name="trait" />
	<xsl:choose>
	<xsl:when test="$trait='Noun'">n</xsl:when>
	<xsl:otherwise><xsl:value-of select="$trait"/></xsl:otherwise>
	</xsl:choose>
	</xsl:template>

	<xsl:template name="abbreviate_slot">
	<xsl:param name="slot" />
	<xsl:choose>
	<xsl:when test="$slot='Verb-slot'">slot</xsl:when>
	<xsl:when test="$slot='inflection- feature'">infl. feat.</xsl:when>
	<xsl:when test="$slot='Noun-infl-class'">cl.</xsl:when>
	<xsl:otherwise><xsl:value-of select="$slot"/></xsl:otherwise>
	</xsl:choose>
	</xsl:template>

</xsl:stylesheet>
