.. See index.rst for info on attribmap, saml, and map directives.

============
Introduction
============

Attribute mapping policies allow you to integrate with Rackspace
identity federation without having to make significant configuration
changes to your identity provider. An attribute mapping policy
provides a declarative means of extracting and transforming
information produced by your identity system so that it may seamlessly
inter-operate with Rackspace.

This document describes the attribute mapping policy language in
detail. It is intended as a guide to assist in the writing of mapping
policies as well as a reference for the features of the policy
language.

Technology Background
---------------------

In order to write attribute mapping policies you should have
a basic understanding of the following technologies:

SAML 2.0
   The Security Assertion Markup Language is an OASIS Standard for
   defining XML-encoded assertions about authentication,
   authorization, and related attributes. A basic understanding of the
   SAML protocol is required, although this document concentrates
   solely on SAML Responses and SAML Assertions.

XPath 3.1
   XPath is a W3C standard expression language for extracting
   information from structured data. The language is designed to be
   embedded in a host language and it is used in this way by the
   mapping policy language. The datatypes and function libraries
   defined by the XPath standard are used in other policy languages
   such as XACML. In most cases, only basic understanding of XPath is
   required, see :doc:`xpath` for a quick overview.

YAML 1.1
   YAML is a simple data serialization language that is designed to be
   human friendly.  YAML is very similar to JSON but allows for useful
   features such as comments and the ability to easily input
   multi-line data. Attribute mapping policies are written in YAML,
   see :doc:`yaml` for a quick overview.


What is Attribute Mapping?
--------------------------

Your identity provider contains information about every user in your
organization.  For example, it maintains that Jane Doe is a
development manager whose username is ``janed``. Jane's email address
is janed@widgets.com, and as a development manager she is a member of
the ``engineering``, ``managers``, and ``linux_user``
groups. Additionally, Jane is managing the ``widgets_ui`` project.

When Jane logs into Rackspace your organization's identity provider
submits to Rackspace a cryptographically signed SAML response that
contains (among other things) the *attributes* described
above. Rackspace (the service provider) needs these attributes in
order to grant Jane access to Rackspace services.

Attribute mapping allows the extraction and transformation of the
attributes in the SAML response so they can be processed by
Rackspace. There are three major reasons why attribute mapping is
required:

Attribute Name Alignment
........................

Rackspace expects that Jane's email is supplied in an attribute named
``email``, but your organization's identity provider by default
submits a user's email in an attribute named ``mail``. This is not an
atypical situation because several competing standards exist for
attribute names. In this case the attribute ``mail`` must be mapped to
the attribute ``email``.

The mapping is simple and can be summarized as follows:

``mail`` → ``email``

Role and Group Alignment
........................

The groups ``engineering``, ``managers``, and ``linux_user`` are
entirely meaningless to Rackspace.  Rackspace organizes its roles
according to the services it provides. For example, the ``nova:admin``
role grants administration access to our OpenStack compute service and
the ``ticketing:observer`` role grants view only access to Rackspace
tickets.

In this case, we want the ``managers`` group to map to the
``ticketing:admin`` role because any manager should be able to create
and edit tickets. We also want to map managers to the
``billing:observer`` role because all managers can see invoices.
Additionally, we want to map the ``linux_user`` group to
``nova:observer`` because all linux users should be able to query the
Compute API.

The mapping described above can be summarized as follows:

``managers``    → ``ticketing:admin``,  ``billing:observer``

``linux_user``  → ``nova:observer``

Implementation of Access Policies
.................................

The mapping above is fairly simple.  All users in the ``managers``
group will obtain the ``ticketing:admin`` and ``billing:observer``
roles. Unfortunately, things are often more complicated than this. For
example, some managers are actually contractors, and contractors
should not be allowed to view invoices so these contractors should not
receive the ``billing:observer`` role. You can tell that a user is a
contractor by looking at membership in the ``contractors`` group.

Additionally, there are separate Rackspace accounts for each project
managed by a manager. A manager involved with the ``widgets_ui``
project should have full administrator rights (via the ``admin`` role)
to account ``777654`` |---| which is the account associated with that
project.  The identity provider passes an attribute named
``manager_projects`` that contains the list of all of the projects
managed by a user.

There are two other projects to consider: ``widgets_moble`` is
associated with account ``887655``, and ``widgets_platform`` which
should have admin access to account ``779966``.

Note that by performing this mapping, you are implementing an access
policy that is executed whenever a user logs in. As long as relevant
information is provided by the identity provider in a SAML response
you can implement similar policies for your organization.

The mapping rules above can be summarized as follows:


``managers`` → ``ticketing:admin``,  ``billing:observer`` unless the
user is also a member of ``contractors``.

``manager_projects`` contains ``widgets_ui``    → ``admin`` on
``777654``

``manager_projects`` contains ``widgets_moble`` → ``admin`` on
``887655``

``manager_projects`` contains ``widgets_platform`` → ``admin`` on
``779956``


Mapping Policy for Widget.com
-----------------------------

The following attribute mapping policy implements the rules described
in the previous section. The rest of this document provides a guide
for writing such polices.

.. map:: widgets/widgets-31.yaml
    :caption: Mapping policy for Widget.com


