Основная информация по пользованию этой библиотекой может быть обнаружена по адресу
{yourapplication}/cache/info
Для подключения кэша к приложению необходимо подключить библиотеку и переопределить настройки cacheopts.properties.
Как их переопределить описано в классе CacheRestFacade.

This cache intercepts URL from any web-app it is connected to. 
On application start it is initialized with settings defined in cacheopts.properties.
E.g:
enabled=true   - if not 'true', do no caching
jerseyEnabled=true   - if not 'true', do no start(this) REST facade
excludeParams=excludeparam  -  these params would be cut off on url cacheable check('../aUrl?excludeparams=3' --> '../aUrl')
denyParams=denyparam  - if one of these params presents, do not cache even if pattern matches('../bUrl?denyparam' --> do not cache)
cachingUrls=aUrl excludeRemove:orgId denyAdd:denyparam=.+&anotherdeny,bUrl   -  here checking patterns go. 
Can include regexp for parameter values like 'denyparam=.+', here only non-empty parameter with name 'denyparam' matches.
The most specified is in priority: if we get 'cacheingUrls=a/b excludeAdd=specifiedExclude,a*', 
then url '../a/b?specifiedExcude=1' turns into '../a/b', but '../a/c?specifiedExcude=1' does not change.
basicPath=Pgu/MfcRestAdapterServlet  - this is basic path prepending Url of request
restPath=MfcDbRestServices/webresources  - if this parameter specified, then url parsing is made upon 
url taken from 'restParam' and with this value as basicPath
restParam=rest