# Releases #
## In Progress Work ##
1. Fixed a bug where multi-value attributes were not always returned as an array when generating JSON output.

## Release 2.0.1 (2017-09-28) ##
1. Fixed a number of parsing bugs when using {(P|A)ts?()} in a template that spans multiple lines.

## Release 2.0.0 (2017-09-25) ##
1. Added the ability to specify default values ({D}) via configuration (defaults.xml) for standard attributes. Several places may be searched for values via XPath, SAML attribute name, or via passed in parameters.
1. Updated Dependency
    1. checker-util: 2.4.0 → 2.4.1

## Release 1.3.1 (2017-08-29) ##
1. Updated Dependencies
    1. saxon: 9.7.0-8 → 9.8.0-4
    1. wadl-tools: 1.0.33 → 1.0.37
    1. checker-util: 2.1.1 → 2.4.0
1. Fixed a bug where we checked XPath syntax against XPath 3.1 but only supported 2.0 -- we now support XPath 3.1 fully.

## Release 1.3.0 (2017-08-14) ##
1. Fixed a bug when converting values with {(P|A)ts()} from XML to JSON
1. Added JSON to YAML conversion with CLI support.
1. Disallowing available-environment-variables, collection, document, environment-variable, unparsed-text-available, unparsed-text, unparsed-text-lines, and uri-collection functions in policy path expressions.
   Also disallowing any functions not in the standard fn or mappingRules namespaces.

## Release 1.2.0 (2017-07-10) ##
1. Added support for {Pt()} and {Pts()} which provides direct access to XPath in a template.
1. Added support for YAML policies

## Release 1.1.1 (2017-06-07) ##
1. Fixed a bug where supported policy XPath functions were not properly declared

## Release 1.1.0 (2017-06-05) ##
1. Clean up : Better reporting of failed tests
1. Clean up : Reuse common assertions in test suite
1. Clean up : Add .DS_Store to .gitignore
1. Fixed bug with {D} on role attribute if a role value contained a space
1. Fixed bug in JSON to XML Conversion of attribute policy
1. Added new call to validate policy given a JSONNode
1. Added XPath validation to disallow doc and doc-available functions in policy path expressions
1. Added support for {At()} and {Ats()} which provides direct access to attributes in a template

## Release 1.0.2 (2017-02-20) ##
1. Fixed a bug where an empty extension attribute value creates a malformed SAMLResponse
1. Fixed a bug where specifying an attribute name that started with a digit caused a failure
1. Fixed a bug where the correct ClassLoader was not properly setup in Saxon
1. Clean up : Added project directories to ensure that all projects can compile locally

## Release 1.0.1 (2017-02-03) ##
1. Set the issue to the SAML Response to a default value of http://openrepose.org/filters/SAMLTranslation

## Release 1.0.0 (2017-02-01) ##
1. Initial Release
