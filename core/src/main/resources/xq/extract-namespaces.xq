xquery version "3.1" encoding "UTF-8";

declare namespace mapping = "http://docs.rackspace.com/identity/api/ext/MappingRules";

<mapping:remoteNS>
  {
      for $remote in //mapping:remote/mapping:attribute[@path]
      return
        mapping:validate-xpath(
          <mapping:remote path="{$remote/@path}">
            {
              for $pfix in in-scope-prefixes($remote) return
                <mapping:ns prefix="{$pfix}" uri="{namespace-uri-for-prefix($pfix,$remote)}"/>
            }
          </mapping:remote>
        )
  }
</mapping:remoteNS>
