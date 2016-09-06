/**
 *  © Copyright IBM Corporation 2014, 2016.
 *  This is licensed under the following license.
 *  The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 *  U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

import com.urbancode.air.CommandHelper;
import com.urbancode.air.AirPluginTool;
import groovyx.net.http.HTTPBuilder;
import static groovyx.net.http.Method.GET;
import static groovyx.net.http.ContentType.JSON;

def apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties();

def application = props['application'] != "" ? props['application'] : null
def exportLocation = props['exportLocation'] != "" ? props['exportLocation'] : null

def ucdhost = props['ucdhost'] != "" ? props['ucdhost'] : null
def ucdport = props['ucdport'] != "" ? props['ucdport'] : null
def ucdusername = props['ucdusername'] != "" ? props['ucdusername'] : null
def ucdpassword = props['ucdpassword'] != "" ? props['ucdpassword'] : null

final File PLUGIN_HOME = new File(System.getenv().get("PLUGIN_HOME"))

//

final def invokeHttp(String url, String username, String password) {

    def http = new HTTPBuilder(url)

    http.ignoreSSLIssues()

    http.request(GET, JSON) { req ->
        headers.'Authorization' = 'Basic ${' + (username + ':' + password).bytes.encodeBase64().toString() + '}'

        response.success = { resp, json ->
            return json
        }

        response.failure = { resp ->
            println 'Unexpected error: ' + resp.status + ':' + resp.statusLine.reasonPhrase
            System.exit(-1)
        }
    }
}

final def getResourceName(String resource) {

    def lastSlash = -1
    for (int i=resource.length()-1; i >= 0; i--) {
        if (resource.charAt(i) == '/') {
            lastSlash = i
            break
        }
    }

    if (lastSlash == 0) {
        return ""
    }
    else {
        return resource.substring(lastSlash + 1)
    }
}

final def getResourceProperties(String app, String env, String branch, String exportLocation, String host, String port, String username, String password) {

    def resourcename = getResourceName(branch)
    if (resourcename != "") {
        def url = host + ':' + port + '/cli/resource/getProperties?resource=' + branch
        def json = invokeHttp(url, username, password)

        def filename = exportLocation + "/" + app + "/" + env + "/" + resourcename + ".properties"
        def file = new File(filename)
        file.withWriter { out ->
            json.each { it ->
                out.writeLine(it.name + "=" + it.value)
            }
        }
    }
}

final def getResource(String app, String env, String branch, String exportLocation, String host, String port, String username, String password) {

    println 'Processing resource: ' + branch

    getResourceProperties(app, env, branch, exportLocation, host, port, username, password)

    def url = host + ":" + port + "/cli/resource?parent=" + branch
    def json = invokeHttp(url, username, password)

    json.each { it ->
        if (it.type != "agent") {
            getResource(app, env, it.path, exportLocation, host, port, username, password)
        }
    }
}

final def getEnvironmentProperties(String app, String env, String exportLocation, String host, String port, String username, String password) {

    def url = host + ':' + port + '/cli/environment/getProperties?environment=' + env + "&application=" + app
    def json = invokeHttp(url, username, password)

    new File(exportLocation + "/" + app + "/" + env).mkdirs()

    def filename = exportLocation + "/" + app + "/" + env + "/" + "environment.properties"
    def file = new File(filename)
    file.withWriter { out ->
        json.each { it ->
            out.writeLine(it.name + "=" + it.value)
        }
    }
}

final def getEnvironment(String app, String env, String exportLocation, String host, String port, String username, String password) {

    println 'Processing environment: ' + env

    getEnvironmentProperties(app, env, exportLocation, host, port, username, password)

    def url = host + ":" + port + "/cli/environment/getBaseResources?environment=" + env + "&application=" + app
    def json = invokeHttp(url, username, password)

    json.each { it ->
        getResource(app, env, it.path, exportLocation, host, port, username, password)
    }
}

final def getApplicationProperties(String app, String exportLocation, String host, String port, String username, String password) {

    def url = host + ':' + port + '/cli/application/getProperties?application=' + app
    def json = invokeHttp(url, username, password)

    new File(exportLocation + "/" + app).mkdirs()

    def filename = exportLocation + "/" + app + "/" + "application.properties"
    def file = new File(filename)
    file.withWriter { out ->
        json.each { it ->
            out.writeLine(it.name + "=" + it.value)
        }
    }
}

final def getApplication(String app, String exportLocation, String host, String port, String username, String password) {

    println 'Processing application: ' + app

    getApplicationProperties(app, exportLocation, host, port, username, password)

    def url = host + ":" + port + "/cli/application/environmentsInApplication?application=" + app
    def json = invokeHttp(url, username, password)

    json.each { it ->
        getEnvironment(app, it.name, exportLocation, host, port, username, password)
    }
}

//

println "Export started."

getApplication(application, exportLocation, ucdhost, ucdport, ucdusername, ucdpassword)

println "Export executed successfully."
System.exit(0)
