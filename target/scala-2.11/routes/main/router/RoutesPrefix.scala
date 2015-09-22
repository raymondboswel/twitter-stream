
// @GENERATOR:play-routes-compiler
// @SOURCE:/home/raymond/activator-1.3.5-minimal/twitter-stream/conf/routes
// @DATE:Tue Sep 22 20:09:39 SAST 2015


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
