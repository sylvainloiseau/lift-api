# Issues in LIFT specifications

Numbers between parentheses refer to page in the LIFT specification : **Lexicon Interchange Format, A Description;  Martin Hosken and Stephen McConnel, version 0.15**

## ID

ID only on entry and sense for now: add them one example, or even on all components?

## Ambiguity of `field-definition`

`(Despite its name, a field-definition may apply to a trait as well as to a field.)` (11)

Create an explicit `trait-definition`.

Make more normative the list of possible data model for trait; make explicit the date format.

## Ambiguity of `trait/@name`

"Where no range is linked the name is informal or resolved by its use in a field-definition." (12)

Use a different attribute for the three cases.

## Naming

range, range-element -> featureset, feature

## External files

External header ranges: prevent renaming, in case the files are shared accross several dictionaries.

## @href on <range> in the header.

Spec says (21):

> This attribute may not be used within an external range
> definition file. In a standard LIFT file, the href attribute may be used to
> reference an external lift-ranges file that contains a definition for this range.
> Any children to this range element in the LIFT file override the values (by
> addition or replacement) in the external range definition. In practice, if the
> range element has an href attribute the id and href values are usually the
> only information given in the LIFT file: all other information is given in the
> referenced Lift Ranges file, with the id attribute repeated to link from the Lift
> Ranges file back to the LIFT file..

How is suppression of a range-element expressed?
