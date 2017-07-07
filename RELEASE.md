# Releases #

## In Progress Work ##
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
