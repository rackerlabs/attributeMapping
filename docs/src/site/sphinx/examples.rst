.. See index.rst for info on attribmap, saml, and map directives.

==========================
Attribute Mapping Examples
==========================

Working with defaults
---------------------

.. attribmap:: defaults
   :saml: sample_assert.xml
   :map: defaults.yaml


Accessing default from a different field:
.........................................

.. attribmap:: defaults2
   :saml: sample_assert.xml
   :saml-show: false
   :map: defaults2.yaml

More complex example with multiple substitutions
................................................

.. attribmap:: defaults3
   :saml: sample_assert.xml
   :saml-show: false
   :map: defaults3.yaml

Mixing in non-default attributes
................................

.. attribmap:: defaults4
   :saml: sample_assert.xml
   :saml-show: false
   :map: defaults4-s.yaml


Working with expiration
-----------------------

Working with lists
------------------
         
Black lists
...........

White lists
...........


