/**
 *  © Copyright IBM Corporation 2014, 2016.
 *  This is licensed under the following license.
 *  The Eclipse Public 1.0 License (http://www.eclipse.org/legal/epl-v10.html)
 *  U.S. Government Users Restricted Rights:  Use, duplication or disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
 */

import com.urbancode.air.AirPluginTool

//Default to declare step properties and location
final def workDir = new File('.').canonicalFile
final def airTool = new AirPluginTool(args[0], args[1])
final def stepProps = airTool.getStepProperties()

//Properties to declare variables from user input
final def dirOffset = stepProps['dirOffset']
final def propertyKeys = stepProps['propertyKeys'].split("\n") as List
final def failWithoutMatch = stepProps['failWithoutMatch'].toBoolean()

if (!dirOffset) {
    dirOffset = '.'
}

//Properties for the processing of the property file
final def propertyFileLoaded = new Properties();
final def propertyFilePath = stepProps['includes']
final def propertyFile = new File(propertyFilePath);
try {
    propertyStream = new FileInputStream(propertyFile);
    propertyFileLoaded.load(propertyStream);
}
catch (IOException e) {
    throw new RuntimeException(e);
}

//Start of functionality
println "Working Directory: ${workDir}"
println "Directory Offset: ${dirOffset}"
println "File Includes: ${propertyFilePath}"
println "-----------------------------"
def failFlag = false

try {
    propertyKeys.each { prop ->
        def result = false

        propertyFileLoaded.each{ cur ->
            if (cur.key.equals(prop) && !result) {
                airTool.setOutputProperty(cur.key, cur.value)
                result = true
            }
        }

        //Figure out the fail
        if (!result && failWithoutMatch) {
            failFlag = true
        }
    }

    airTool.setOutputProperties()
}
catch (Exception e) {
    e.printStackTrace()
    System.exit 1
}

if (!failFlag) {
    System.exit(0)
}
else {
    println "Some properties were not found. Exiting Failure"
    System.exit(1)
}
