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
        b : env.BUILD_NUMBER
        p : BUILD_PARAM
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

import org.jenkinsci.plugins.simplebuild.SimpleBuild;

// The call(body) method in any file in workflowLibs.git/vars is exposed as a
// method with the same name as the file.
def call(body) {
    def config = new org.jenkinsci.plugins.simplebuild.SimpleBuild()
    //def code = body.rehydrate(config, body.getOwner(), this)
    //code.resolveStrategy = Closure.DELEGATE_FIRST
    //System.out.println("System code()")
    //echo("code()")
    //code()
    System.out.println("System body()")
    echo("body()")
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    //body.resolveStrategy = Closure.DELEGATE_FIRST
    //body.delegate = config
    //echo(body.getOwner().getClass().getName())
    //echo(body.getOwner().env)
    //body()
    
    //echo(body.getClass().getName())
    //echo(code.gitRepo)
    //echo(code.map)
    echo(body.gitRepo)
    echo(body.map)
    echo(config.gitRepo)
    echo(config.map)

    /** Run the build scripts */

    try {
        if (body.docker_image != null) {
            runViaDocker(body)
        } else {
            runViaLabel(body)
        }
    } catch (Exception rethrow) {
        failureDetail = failureDetail(rethrow)
        sendMail(body, "FAILURE: Pipeline '${env.JOB_NAME}' (${env.BUILD_NUMBER}) failed!",
                "Your job failed, please review it ${env.BUILD_URL}.\n\n${failureDetail}")
        throw rethrow
    }

    /** conditionally notify - maybe wih a catch */
    sendMail(body, "Pipeline '${env.JOB_NAME}' (${env.BUILD_NUMBER}) succeeded.",
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
        echo(this.getClass().getName())
        echo(config.git_repo)
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
