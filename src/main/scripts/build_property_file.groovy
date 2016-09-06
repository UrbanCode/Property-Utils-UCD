/**
 *  © Copyright IBM Corporation 2014, 2016.
 *  This is licensed under the following license.
 *  The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 *  U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

import com.urbancode.air.CommandHelper;
import com.urbancode.air.AirPluginTool;
import static groovy.io.FileType.FILES

def apTool = new AirPluginTool(this.args[0], this.args[1])
def props = apTool.getStepProperties();

def sourcedir = props['sourcedir'] != "" ? props['sourcedir'] : null
def includes = props['includes'] != "" ? props['includes'] : null
def targetfilename = props['targetfilename'] != "" ? props['targetfilename'] : null
def extension = props['extension'] != "" ? props['extension'] : null

final File PLUGIN_HOME = new File(System.getenv().get("PLUGIN_HOME"))

//

def getPropertyFiles( sourcedir, includes, extension ) {

    println 'Generating property file list ...'

    files = []

	includeFiles = includes.split("\n") as List

    new File( sourcedir ).eachFileRecurse(FILES) {
        if(it.name.endsWith( extension )) {
			println 'Found property file: ' + it.path

			includeFiles.each { includeFile ->
				if ( includeFile == it.name ) {
					println 'Adding property file: ' + it.path

					files << it
				}
			}
        }
    }

    return files
}

def buildPropertyFile( targetfilename, files ) {

    println 'Building new property file ...'

    new File( targetfilename ).withWriter { writer ->
        files.each { file ->

            println 'Appending property file: ' + file.path

            new File(file.path).withReader { reader ->
                if (reader) {
                    writer << reader
                }
            }
        }
    }
}

//

println "Build property file started."

files = getPropertyFiles( sourcedir, includes, extension )
buildPropertyFile( targetfilename, files )

println "Build property file executed successfully."
System.exit(0)
