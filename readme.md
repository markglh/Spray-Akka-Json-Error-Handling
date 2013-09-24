##Error handling with Spray (JSON!!)

Following on from Jan's earlier blogs which detail and use a nice error handling mechanism for Spray:  [errors & failures](http://www.cakesolutions.net/teamblogs/2012/12/10/errors-failures-im-a-teapot/) and [AngularJS in Akka and Spray](http://www.cakesolutions.net/teamblogs/2013/08/05/angularjs-in-akka-and-spray/) (Now a typesafe activator template). I'm now using this in a production application so the plan is to dive a bit deeper by refining the error handling and elaborating a bit more on the important parts.

###Structural changes
First this first, compared to the previous blogs i'll outline what changes I've made to the code structure.

- More comments - Pretty much speaks for itself 
- Added a default error selector - Undefined instances of ```Left``` will automatically fallback to the default handler
- Restructured code packages slightly - Moved boiler plate code into ```scaffolding``` package
- Tweaked ```RoutedHttpService``` to delegate error and rejection handling to the ```FailureHandling``` ```Trait```
- Added ```FailureHandling``` ```Trait```- The error handling now falls back on the ```spray.routing.ExceptionHandler``` default cases rather than overriding them unnecessarily. All rejections and exceptions are automatically converted to JSON.



#TODO

replaced the rejection handler to pass back to the master for standard errors

lots of examples of request/responses

Dont think I've done the following:
Refactored so that all error responses extend a supertype rather than being discrete objects

