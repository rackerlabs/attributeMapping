# AttributeMapping

## Intro

This project contains a an implementation of the Mapping Combinations
policy format that's utilized by OpenStack to map an Identity
Provider's Federated Attributes to local Attributes understood by the
service providers implementation.

A description of the Mapping Combinations policy format can be found
here:

http://docs.openstack.org/developer/keystone/federation/federated_identity.html#mapping-combinations

There are some differences between the OpenStack format described
above and the format implemented here (these are described below)
however the concepts and much of the format is compatible.

The main idea is that a SAML Request is sent to the identity
provider -- if a Mapping Policy existis then the policy translates
this SAML Request into one the Identity System can understand.

The current implementation is non-destructive which means it simply
adds a new Assertion to the Request that contains only the mapped
attributes -- with the idea that the Identity System will process the
new assertion.  The initial assertion is kept on the request -- and
the signiture of that assertion can still be verified.

## Building CLI utilites

You can interact with the polcy engine with CLI utilites to build
these utilities you will need.

1. Java 1.8
2. Maven 3.3 (https://maven.apache.org/download.cgi)
3. GCC C compiler

Once you've intalled these in the main directory simply type:

````shell
mvn install
````

You'll need to have Maven and Java in the system path.  Next add the
```bin``` directory to your system path. 

## CLI utilites

1. attribmap : given a Mapping Policy and a SAML Request -- displays
standard attributes according to the mapping.  OR if the ```-s```
flaged is used outputs a modifed SAML Request with a new assertion.

1. attribmap2xml : converts a JSON policy to XML, cus XML is
beautiful.

1. attribmap2json : converts an XML policy to JSON, cus JSON rocks!

1. attribmap2xsl : converts a Mapping Policy to an XSL 2.0 stylesheet
that can be used to efficently translate SAML Request so that it
contains a new assertion with the mapped attributes.

## Differences from OpenStack Policy

TODO

## Tests

There are many examples of polices and related assertions here:

https://github.rackspace.com/jorge-williams/attributeMapping/tree/master/core/src/test/resources/samples

These are slowly being migrated into formal test suites here:

https://github.rackspace.com/jorge-williams/attributeMapping/tree/master/core/src/test/resources/tests

Each test suite is a directory with two subdirectories:

1. ```maps``` : contain different versions of a Mapping Policy which
are functionally equivilant.

2. ```asserts``` : contain different SAML assertions that will work
with the policy.

The SAML assertions are annotated with XPath 2.0 expressions at the
beginning of the file like this:

````xml
<?xml version="1.0" encoding="UTF-8"?>

<!-- There should be two assertions -->
<?assert count(/saml2p:Response/saml2:Assertion) = 2 ?>

<!-- There should be 2 roles -->
<?assert count(mapping:get-attributes('roles')) = 2?>

<!-- The roles should be nova:observer and lbaas:observer -->
<?assert mapping:get-attributes('roles')='lbaas:observer' and mapping:get-attributes('roles')='nova:observer' ?>

<!-- None of the roles should not be lbaas:admin or nova:admin -->
<?assert every $r in mapping:get-attributes('roles') satisfies not($r = ('lbaas:admin', 'nova:admin'))?>

<!-- The name should be john.doe -->
<?assert /saml2p:Response/saml2:Assertion[1]/saml2:Subject/saml2:NameID = 'john.doe'?>

<!-- The message should expire at 2013-11-17T16:19:06.298Z -->
<?assert /saml2p:Response/saml2:Assertion[1]/saml2:Subject/saml2:SubjectConfirmation/saml2:SubjectConfirmationData/@NotOnOrAfter = '2013-11-17T16:19:06.298Z'?>

<!-- The email should be  John Doe &lt;john.doe@323676.rackspace.com&gt; -->
<?assert mapping:get-attribute('email') = 'no-reply@rackspace.com'?>

<!-- The email should be no-reply@rackspace.com -->
<?assert mapping:get-attribute('domain') = '323676'?>

<saml2p:Response ID="_7fcd6173-e6e0-45a4-a2fd-74a4ef85bf30" 
````

These assertions are made are on the converted SAML an the test passes
only if they all validate to true.

There are a numeber of built in functions that XPath implementation
understands:

1. ```mapping:get-attributes()```  : Takes a name of an attribute and
returns sequence of all of its values -- only in the FIRST
(translated) assertion.

1. ```mapping:get-attribute()```  : Does the same as above but only
returns the first value.

1. ```mapping:get-expire()``` : Gets the expiration time as an
```xs:dateTime`` so you can do assertions based on time. 

If a test fails take a look in the directory ```core/target/surefire-reports/```.

That directory will contain txt files with a detailed explanation on
why a test failed.

## Resources

XPath 2.0 is used by this implementation of the Mapping Policy. You
can find a list of XPath 2.0 function here:

http://www.w3schools.com/xml/xsl_functions.asp

TODO : other resoures


