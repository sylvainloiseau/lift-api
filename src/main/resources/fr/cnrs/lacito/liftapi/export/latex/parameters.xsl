<?xml version="1.0" encoding="utf-8"?>
<!--
 | The parameters of the LaTeX export.
 |
 | They are global parameters of lift2latex.xsl, which imports this file. Set them from
 | outside: from Java (fr.cnrs.lacito.liftapi.export.LatexExport), from lift-api's command
 | line (lift-api latex), or with any XSLT 1.0 processor (xsltproc: its stringparam
 | option). Their names are also the keys of the command line's JSON settings file.
 -->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<!-- The title and the author, on the title page. -->
	<xsl:param name="title" select="''"/>
	<xsl:param name="author" select="''"/>

	<!--
	 | Booleans. A parameter set from outside may be a string: xsltproc's stringparam
	 | makeFigure false gives the string 'false', which XSLT 1.0 takes as true. They are
	 | read through the template "is-true" (lift2latex.xsl), which accepts true() and
	 | 'true' only.
	 -->
	<!-- A second part, with the entries grouped by semantic domain (semantic-domain-ddp4). -->
	<xsl:param name="makeThematicDictionary" select="true()"/>
	<!-- A list of figures at the end. -->
	<xsl:param name="makeListOfFigure" select="false()"/>
	<!-- The illustrations of the senses, as figures. -->
	<xsl:param name="makeFigure" select="false()"/>

	<!--
	 | Prefixed to the href of each illustration: a directory, ending with "/". Empty: the
	 | hrefs are relative to the .tex file.
	 -->
	<xsl:param name="urlillustration" select="''"/>

	<!--
	 | The polyglossia name of the languages of the dictionary, as code=name pairs
	 | separated by ";", e.g. 'fr=french;en=english;tww=tuwari'. For the names, see the
	 | polyglossia documentation:
	 |   http://tug.ctan.org/macros/xetex/latex/polyglossia/polyglossia.pdf
	 | Each name is declared once (\setotherlanguage), and the texts in that language are
	 | written in it. A language that is not listed is written in the default language.
	 -->
	<xsl:param name="languages" select="''"/>

	<!--
	 | The language whose alphabetical order sorts the entries: a code such as 'fr'. Its
	 | polyglossia name (from "languages") is also the default language of the document
	 | (\setdefaultlanguage); english when it has none.
	 | The JDK's processor sorts with a java.text.Collator for it, which ignores case and
	 | accents at the first level. Empty: English. An empty lang would use the machine's
	 | default locale, so that a file would sort differently on two machines.
	 -->
	<xsl:param name="sortLanguage" select="''"/>

	<!-- The main font (fontspec's \setmainfont), e.g. 'Charis SIL'. Empty: the LaTeX default. -->
	<xsl:param name="mainFont" select="''"/>

	<!--
	 | A font for some polyglossia languages, as name=font pairs separated by ";", e.g.
	 | 'french=Charis SIL;tuwari=Doulos SIL'. Each is written as
	 | \newfontfamily\<name>font{<font>}, which polyglossia uses for that language.
	 -->
	<xsl:param name="fonts" select="''"/>

	<!--
	 | Letters. translate($s, $foldFrom, $foldTo) removes the case and the accents of a
	 | character, to group the entries under their letter; the headings are written with
	 | translate($s, $upperFrom, $upperTo). They cover ASCII, Latin-1 Supplement and Latin
	 | Extended-A; ø, æ, œ, ð, þ, ł, ŋ... stay letters of their own.
	 |
	 | An xsl:key cannot refer to a variable, so lift2latex.xsl repeats foldFrom and foldTo
	 | in its key "letter": change both together (a test checks that they agree). Java reads
	 | these variables (LatexExport.headingLetter), so that it computes the same letters.
	 -->
	<xsl:variable name="foldFrom" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞàáâãäåçèéêëìíîïñòóôõöùúûüýÿĀāĂăĄąĆćĈĉĊċČčĎďĐĒēĔĕĖėĘęĚěĜĝĞğĠġĢģĤĥĦĨĩĪīĬĭĮįİĲĴĵĶķĹĺĻļĽľĿŁŃńŅņŇňŊŌōŎŏŐőŒŔŕŖŗŘřŚśŜŝŞşŠšŢţŤťŦŨũŪūŬŭŮůŰűŲųŴŵŶŷŸŹźŻżŽžſ'"/>
	<xsl:variable name="foldTo" select="'abcdefghijklmnopqrstuvwxyzaaaaaaæceeeeiiiiðnoooooøuuuuyþaaaaaaceeeeiiiinooooouuuuyyaaaaaaccccccccddđeeeeeeeeeegggggggghhħiiiiiiiiiĳjjkkllllllŀłnnnnnnŋooooooœrrrrrrssssssssttttŧuuuuuuuuuuuuwwyyyzzzzzzs'"/>
	<xsl:variable name="upperFrom" select="'abcdefghijklmnopqrstuvwxyzæøðþœđħłŋŧĳŀ'"/>
	<xsl:variable name="upperTo" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÆØÐÞŒĐĦŁŊŦĲĿ'"/>

</xsl:stylesheet>
