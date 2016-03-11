package dsl

// See https://github.com/jenkinsci/workflow-plugin/tree/master/cps-global-lib#defining-global-functions

/* sample with all the things turned on:
<code>
simpleBuild {

    machine = "hi-speed"
    docker = "java:1.9"

    map = [
        FOO : 42,
        BAR : "YASS"
        url : "http://host/path/${PARAM}/${env.BUILD_NUMBER}/tail"
    ]

    git_repo = "https://github.com/cloudbeers/PR-demo"

    before_script = "echo before"
    script = 'echo after $FOO'
    after_script = 'echo done now'

    notifications = [
        email : "mneale@cloudbees.com"
    ]

}
</code>

*/


// The call(body) method in any file in workflowLibs.git/vars is exposed as a
// method with the same name as the file.
def call(body) {
    def config = [:]

    echo("call this: "+this.getClass().getName())
    if (this instanceof Closure) {
        echo("call owner: "+owner.getClass().getName())
        echo("call delegate: "+delegate.getClass().getName())
    }

    // Alternative 1
    // When using Alternative 1, comment out Alternative 2 below
    // This alternative results in java.lang.NullPointerException: Cannot get property 'BUILD_NUMBER' on null object
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    echo("body this: "+body.getClass().getName())
    if (body instanceof Closure) {
        echo("body owner: "+body.owner.getClass().getName())
        echo("body delegate: "+body.delegate.getClass().getName())
    }
    echo("Start: body()")
    body()
    echo("Done: body()")

/*
    // Alternative 2
    // When using Alternative 2, comment out Alternative 1 above
    // This alternative results in groovy.lang.MissingPropertyException: No such property: scm for class: groovy.lang.Binding
    def code = body.rehydrate(config, body.getOwner(), this)
    code.resolveStrategy = Closure.DELEGATE_FIRST
    echo("code this: "+code.getClass().getName())
    if (code instanceof Closure) {
        echo("code owner: "+code.owner.getClass().getName())
        echo("code delegate: "+code.delegate.getClass().getName())
    }
    echo("Start: code()")
    code()
    echo("Done: code()")
*/

    /** Run the build scripts */

    try {
        if (config.docker_image != null) {
            runViaDocker(config)
        } else {
            runViaLabel(config)
        }
    } catch (Exception rethrow) {
        failureDetail = failureDetail(rethrow)
        sendMail(config, "FAILURE: Pipeline '${env.JOB_NAME}' (${env.BUILD_NUMBER}) failed!",
                "Your job failed, please review it ${env.BUILD_URL}.\n\n${failureDetail}")
        throw rethrow
    }

    /** conditionally notify - maybe wih a catch */
    sendMail(config, "Pipeline '${env.JOB_NAME}' (${env.BUILD_NUMBER}) succeeded.",
            "Be happy. Pipeline '${env.JOB_NAME}' (${env.BUILD_NUMBER}) succeeded.")


}

def sendMail(config, mailSubject, message) {
    /*
     * We have to build a primitive list up so we can use simple iteration
     * so that things can be serialized as per continuation passing style
     */
    emailList = []
    if (config.notifications != null) {
        for ( e in config.notifications ) {
            if (e.getKey() == "email") {
                emailList.add(e.getValue());
            }
        }
    }

    for (i = 0; i < emailList.size(); i++) {
        mail body: message, subject: mailSubject, to: emailList[i]
    }
}


/** Execute the scripts on the appropriate label node */
def runViaLabel(config) {
    node(config.machine) {runScripts(config)}
}

def runViaDocker(config) {
    node(config.machine) {
        docker.image(config.docker_image).inside {
            runScripts(config)
        }
    }
}


/** Run the before/script combination */
def runScripts(config) {
    envList = []
    for ( e in config.map) {
        envList.add("${e.getKey()}=${e.getValue()}")
    }
    withEnv(envList) {

        /* checkout the codes */
        if (config.git_repo == null) {
            checkout scm
        } else {
            git config.git_repo
        }

        /* run the basic build steps */
        if (config.before_script != null) {
            sh config.before_script
        }
        sh config.script
        if (config.after_script != null) {
            sh config.after_script
        }


    }
}

/**
 * Read the detail from the exception to be used in the failure message
 * https://issues.jenkins-ci.org/browse/JENKINS-28119 will give better options.
 */
def failureDetail(exception) {
    /* not allowed to access StringWriter
    def w = new StringWriter()
    exception.printStackTrace(new PrintWriter(w))
    return w.toString();
    */
    return exception.toString()
}
