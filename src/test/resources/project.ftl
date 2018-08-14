<#include "searchuinew">

[
<#list results as el>
    {
        "title": <@value el "search:title"/>,
        "description": <@value el "search:description"/>,
        "publications": <@for el "search:publications" ; pub>
            <#assign url_string=get_value(pub "search:citation")+"\n[DOI: "+get_value(pub "search:doi")+"]\n[DOI: "+get_value(pub "search:doi")+"]: https://doi.org/"+get_value(pub "search:doi")?url />
            <@direct_value url_string/>
        </@for>,
        "dataset":<@ref "Dataset" el "search:datasets" "search:identifier" "search:name" "search:uuid"/>,
        "license_info": <@link instance=el path="search:licenses" url="search:url" label="search:name" distinctPropertyName="search:url"/>,
        "allfiles": <@fileDownload el "search:datasets" "search:containerUrl" {"text": "download all related data as ZIP"}/>,
        "contributors": <@ref "Person" el "search:contributors" "search:identifier" "search:name" "search:uuid" "search:identifier"/>
    }
<#sep>,
</#list>
]