xquery version "3.1" encoding "UTF-8";

declare namespace mapping = "http://docs.rackspace.com/identity/api/ext/MappingRules";

mapping:validate-xpath(
        <mapping:policy>
            <mapping:namespaces>
                {
                    let $root := /mapping:mapping
                    for $pfix in in-scope-prefixes($root)
                    return
                        <mapping:namespace prefix="{$pfix}" uri="{namespace-uri-for-prefix($pfix, $root)}"/>
                }
            </mapping:namespaces>
            <mapping:remotes>
                {
                    for $remote in //mapping:remote/mapping:attribute[@path]
                    return
                        <mapping:remote path="{$remote/@path}"/>
                }
            </mapping:remotes>
        </mapping:policy>
)
