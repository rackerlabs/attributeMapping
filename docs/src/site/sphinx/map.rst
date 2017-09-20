.. See index.rst for info on attribmap, saml, and map directives.

========================
Attribute Mapping Basics
========================

Attribute mapping policies describe a means of extracting a set of
well known identity attributes from a signed SAML assertion produced
by an Identity Service Provider (IDP).  We'll begin this section by
looking at a sample SAML assertion in detail, we'll then describe the
attributes that identity needs to extract from the assertion in order
to operate, and we'll wrap up by walking through the construction of a
mapping policy that extracts those attributes from the assertion.


The SAML Assertion
------------------

When an Identity Provider successfully authenticates a user it
presents Rackspace Identity with a SAML Assertion, much like the one
below:

.. saml:: mapping-rule-exp/sample_assert.xml
   :caption: Example SAML Assertion.

The assertion describes a view of the user that has successfully
logged in. It contains within it all of the information deemed by the
IDP to be relevant to the Service Provider (the SP |---| which in this
case is Rackspace).

.. note::

   For help configuring Third Party identity providers (such as Active
   Directory Federation Services, Okta, and others) please refer to the
   `Rackspace Identity Federation User Guide`_.

Parts of the SAML Assertion
...........................

In this section, we break down the SAML assertion listed above into
its relevant parts. Note first that per the SAML protocol, the
assertion itself is wrapped in a ``<saml2p:Response />`` element which
begins on line 2.  The actual assertion (``<saml2p:Assertion />``)
begins in line 34.

Issuer (37):
  The issuer is the system that generated (or issued) the assertion.
  This is identified as a URI.

Signature (38 |--| 57):
  The XML Signature of the assertion part of the request. The
  signature is used to verify that that the assertion was indeed
  produced by the issuer.

Subject (58 |--| 63):
  The subject is used to identify the identity (or user) that the
  assertion is about.

AuthnStatement (64 |--| 69):
  The AuthnStatement contains details on how the subject
  authenticated.

AttributeStatement (70 |--| 91):
  This section contains a list of arbitrary attributes associated with
  the subject.  Each attribute in the list is essentially a name/value
  pair.  Note, that values are of a type identified by the
  ``xsi:type`` XML attribute |---| in this case they are all strings.
  Also note, that attributes may have multiple values.  The groups
  attribute defined in 80 |--| 84, for example, contains 3 separate values
  (group1, group2, and group3).

Signing SAML Assertions
.......................

Both the SAML Response (2) and the SAML Assertion (34) may be signed.
Rackspace Identity may verify both signatures. It's important to note
however that while signing the SAML Response is optional, signing the
SAML Assertion is strictly required. This means that a message that
contains a single signature at the SAML Response level will be
rejected.

It is possible for a SAML Response to contain multiple assertions. In
this case, all assertions must be signed and they must all be issued
by the same issuer.  Rackspace Identity typically examines only the
first assertion for authorization data, this behavior can be easily
overwritten with a mapping policy.


Required Attributes
-------------------

We now examine the attributes that Rackspace Identity requires in
order to successfully authenticate a user.  As we look through these,
we'll examine where we can retrieve these attributes from the SAML
Response above.

Domain
......

Rackspace Identity keeps information about users, roles, and other
entitlements in a domain. When a user federates into Rackspace, the
user is placed in a single identity domain. Each domain is accessed
via a unique alpha-numeric ID which Rackspace usually sets to be the
same the user's account ID. This ID is required when a federated user
requests access.  This is especially important because a customer is
allowed to create multiple domains and Rackspace Identity needs to
place the federated user in the correct one.

In the SAML Assertion above the domain is passed as a SAML attribute
in lines 74 |--| 76. This implies that the identity provider was
pre-configured to emit the correct value. It is not strictly required
that IDP do this since most federated users target a single domain and
the domain value can be easily hard coded in an attribute mapping
policy as we'll see later in this chapter.

Name
....

This is the username of the federated user.  Rackspace Identity
assumes that each user will be identified with a unique username, and
that the same user will have the same username from one federated
login to the next.

In the SAML Assertion above the username is identified by the
``NameID`` in the Subject section of the assertion (line 59). That
said, it is possible for an IDP to return a stable username as an
attribute in the AttributeStatement section.

Email
.....

This is the email of the federated user.  In the SAML assertion it is
identified as an attribute in the AttributeStatement section (lines
77 |--| 79). Some IDPs will make no distinction between a username and an
email, in which case the email will be located in the Subject section.

Roles
.....

Another attribute expected by Rackspace Identity is the list of roles
that should be assigned to the federated user.  Rackspace Identity
only allows roles that it recognizes to be assigned.  See the
`Rackspace Identity Federation User Guide`_ for the most current `list
of allowed roles`_.

In the SAML Assertion above the list of roles is specified in an
attribute named ``roles`` on lines 71 |--| 73.  Note that this is a
good use case for a multi-value attribute, but in this case we only
assign the ``nova:admin`` role.

We are assigning ``nova:admin`` at the domain level. This
means that the user will have the role on all accounts (sometimes
referred to as tenants) that are associated with the domain.  For
example, if accounts ``12873`` and ``33987`` are associated with the
domain the user will have the ``nova:admin`` role on both of those
accounts.

It is possible to restrict the role to a specific account.  For
example, to grant ``nova:admin`` on account ``12873`` and
``nova:observer`` on account ``33987``, you should specify the roles
as ``nova:admin/33987`` and ``nova:observer/33987``.

Expire
......

Finally, Rackspace identity needs to understand the amount of time
that a federated user should be allowed on Rackspace systems before
the user is forced to re-authenticate.  This attribute can be
expressed in two different formats. First, an `ISO 8601`_ timestamp
may be provided, this timestamp should include a time zone designator.
For example, the timestamp ``2017-10-04T16:20:57Z`` signifies that the
user should be forced to re-authenticate after October 4th 2017 at
16:20:57 UTC.  Secondly, an `ISO 8601`_ duration may be specified.
For example, ``PT1H2M`` signifies that the user should be forced to
re-authenticate one hour and two minuets after successfully logging
in.

In the SAML Assertion above an expire timestamp is specified in the
``NotOnOrAfter`` attribute of the SubjectConfirmationData on line 61.
In SAML, this attribute is meant to denote the time after which the
SAML Assertion should no longer be considered valid. While this
timestamp does not fit semantically with the expire attribute that
Rackspace Identity expects it still works as a reasonable default.

Other Attributes
................

The attributes described in the previous sections (domain, name,
email, roles, and expire) are expected in every federated login. Some
Rackspace products may expect additional optional attributes. Please
consult the `Rackspace Identity Federation User Guide`_ for details on
these attributes.


Mapping Attributes
------------------

So far, we've broken down the SAML Assertion and identified places
where we can find values for the 5 attributes that Rackspace Identity
requires.  This is summarized in the table below:

.. include:: tables/loc.rst

.. attribmap:: mapping-rule-exp
   :saml: sample_assert.xml
   :saml-show: false
   :map: mapping-rule-exp-xpth.yaml
   :map-show: false
   :results-caption: The values at those locations are listed here:

In a sense, this table represents an attribute mapping.  We are
mapping data located in the SAML Assertion into attributes that
Rackspace Identity requires to log in a federated user.  This is a
silly mapping, however, because mapping attributes by referring to
line numbers is extremely unpractical, inexact, and brittle. Using
XPath, on the other hand, is a more stable and practical way of
pinpointing the exact location of the data that we need. After all,
XPath was designed specifically to pinpoint and extract data form XML
documents [#j1]_.

Mapping Attributes with XPath
.............................

In the table below, we replace line numbers with XPaths into the SAML
Assertion.

.. include:: tables/xpath.rst

We can easily turn this table into an attribute mapping policy:

.. map:: mapping-rule-exp/mapping-rule-exp-xpth.yaml

Let's walk through the policy above in detail and examine how XPath is
used to extract the attribute values.

Parts of the Mapping Policy
~~~~~~~~~~~~~~~~~~~~~~~~~~~

The mapping policy is YAML document that contains instructions aimed
at retrieving (or deriving) identity attributes from a SAML
Assertion. You can think of it as a simple script that executes every
time a SAML Assertion is presented to Rackspace Identity. In this
section, we break the mapping policy above into its relevant parts.

mapping (2):
  The mapping policy is always contained in a single top-level
  ``mapping`` object.

version (3):
  The ``version`` key identifies the version of the mapping policy
  language.  It is a required attribute and should always have the
  value of ``RAX-1``. The mapping policy language described here is
  based on the `Mapping Combinations`_ language by the OpenStack
  Keystone and the version name is used to differentiate a Rackspace
  Identity mapping policy from a Keystone mapping policy.

description (4):
  The ``description`` key provides a human readable description of the
  mapping policy. This description is optional.

rules (6):
  A mapping policy is made up of a collection of rules. These rules
  are encapsulated by the ``rules`` array.  A policy is required to
  contain at least one rule.

rule (7 |--| 13):
  Lines 7 |--| 13 contain a rule that drives the policy. A rule may contain
  a ``local`` and a ``remote`` section.  Both ``local`` and ``remote``
  sections are optional (in this case, we don't need a ``remote``),
  however, there should be at least one rule with a ``local`` section.

  It's important to note that things are local or remote from the
  perspective of Rackspace Identity. For example, the ``local``
  section contains statements about the user within Rackspace Identity
  (the local user).  The remote section contains statements about the
  user as its presented by the IDP (the remote user).

  Lines 8 |--| 13 describe what the local user should look like |---| in other
  words they describe the attributes of the local user. Here, we
  specify each of the required identity attributes and describe how
  they can be obtained from an XPath.

Using XPath in the Mapping Policy
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

You'll note that the XPaths in the mapping policy are contained within
something that looks like this ``{Pts()}`` |---| this is known as an
XPath substitution. There are various types of substitutions in the
mapping policy language each of which is encapsulated by curly braces
``{}``. Substitutions are only allowed in the local part of the
mapping policy, they help set values for local attributes in that they
are always replaced (or substituted) by other content. In the case of
the XPath substitution it is replaced by the result of the XPath
within the parenthesis when executed against the SAML Assertion.

It is important to note that spaces matter in a substitution.  In
order to be interpreted correctly there should be no spaces between
the prologue of the substitution ``{Pts(`` and the epilogue of the
substitution ``)}``.

.. include:: tables/pts.rst

You'll notice that the XPath substitution refers to elements in
certain namespaces (``saml2p``, ``saml2``). These namespace prefixes,
which are common to SAML, are always predefined. The table below
describes how namespace prefixes are mapped by default:

.. include:: tables/namespaces.rst

It is possible for a SAML Assertion to refer to elements in extended
namespaces not listed here. You can define additional prefixes by
adding a ``namespaces`` key to the mapping policy. In XPath, it's the
namespace URI that uniquely identifies an element |---| the prefix is
simply a short had for this URI.  In the example below, we replace the
``saml2p`` prefix with ``foo``. Because the namespace URI bound to
element is the same as the previous example the two mapping policies
will produce the exact same result.

.. map:: mapping-rule-exp/mapping-rule-exp-xpth-6.yaml

Using the {Pt()} Substitution
.............................

Notice that the XPath for retrieving the domain ends with
``saml2:AttributeValue[1]`` while the XPath for retrieving the list of
roles ends ``saml2:AttributeValue``.  This is because we want a single
value for a domain, but we want a list of roles.  Without the ``[1]``
the XPath would return every ``AttributeValue`` in a SAML Assertion
for a domain |---| the ``[1]`` signifies that we are only interested
in the first value we find [#j2]_.

Because it is common to want to retrieve a single value, there's an
alternative XPath substitution (``{Pt()}``) that always returns the
first value of an XPath result. This is useful in cases where we
expect a single value and we want to automatically protect against the
off chance that we'll receive multiple values in a SAML assertion.

Given this new substitution, we can rewrite the mapping policy as
follows:

.. map:: mapping-rule-exp/mapping-rule-exp-xpth-2.yaml


Using the mapping:get-attributes Call
.....................................

At this point, the XPaths for retrieving a domain, email, and roles
all look almost exactly the same as each other. They are all
retrieving the list of values for an attribute in the
``AttributeStatement`` of the assertion by name.  Because this is a
common case, there is a predefined XPath function called
``mapping:get-attributes`` that takes the name of a SAML attribute as
a string, and returns the attribute values associated with that name.

We can rewrite the mapping policy using this function as follows:

.. map:: mapping-rule-exp/mapping-rule-exp-xpth-3.yaml

Using the {Ats()} and {At()} Substitutions
..........................................

Having an XPath function that retrieves SAML attribute values is handy
when you want to process those values in some way before returning a
result.  In some cases, however, you simply want to return the value
(or values) of a SAML attribute as is. The ``{Ats()}`` and ``{At()}``
substitutions, do just that. These are known as attribute
substitutions. As with their XPath counterparts, the ``{Ats()}``
substitution returns all values for a specific attribute name and the
``{At()}``.

Given these substitutions we can rewrite the policy as follows:

.. map:: mapping-rule-exp/mapping-rule-exp-xpth-4.yaml

Notice that in this case the name of the attribute is not in single
quotes. We are no longer directly using XPath with attribute
substitutions, although, under the hood, these substitutions are also
interpreted as XPath statements.

Using the {D} Substitution
..........................

There are certain default places where a mapping policy may look for a
particular identity attribute value. The default substitution
(``{D}``) is always replaced with the value at the default
location. Unlike other substitutions the default substitution is
interpreted differently depending on where the substitution is
placed. For example, if the substitution is placed as a value of the
``name`` attribute it will replace itself with the default value for
name. Likewise, if ``{D}`` is placed in the ``email`` attribute it
will be replaced with the default value for email and so on.

What are the default locations in a SAML assertion for the five
attributes Rackspace Identity expects?  It turns out that the SAML
assertion in this chapter has all of the values in the default places!
So it's possible, in this case, to write a mapping policy that looks
like this:

.. map:: mapping-rule-exp/mapping-rule-exp-xpth-5.yaml

Next Steps
----------

So far we've learned about the SAML Assertions and seen how to use
XPath, attribute names, and expected defaults to write mapping
polices.  All of the polices we've created, however, have described
simple direct mappings. In the chapters that follow we'll start
looking at writing polices in cases where things don't align so
perfectly.
         
.. References:

.. _Rackspace Identity Federation User Guide:
   http://developer.rackspace.com/docs/rackspace-federation
.. _list of allowed roles:
   https://developer.rackspace.com/docs/rackspace-federation/attribmapping-basics/full-roles/
.. _ISO 8601:
   https://en.wikipedia.org/wiki/ISO_8601
.. _Mapping Combinations:
   https://docs.openstack.org/keystone/latest/advanced-topics/federation/mapping_combinations.html

.. Footnotes:

.. [#j1] Later versions of XPath allow extracting data from JSON
         documents as well!
.. [#j2] The first index in XPath is 1 not 0.  This logical and makes
         sense unless you're a software developer.
