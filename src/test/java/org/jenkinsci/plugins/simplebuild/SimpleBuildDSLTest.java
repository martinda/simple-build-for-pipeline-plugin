package org.jenkinsci.plugins.simplebuild;

import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.scm.GitSampleRepoRule;
import org.jenkinsci.plugins.workflow.steps.scm.GitStep;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.model.Statement;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import hudson.model.ParametersDefinitionProperty;
import hudson.model.StringParameterDefinition;

public class SimpleBuildDSLTest {


    @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();
    @Rule public RestartableJenkinsRule story = new RestartableJenkinsRule();
    @Rule public GitSampleRepoRule sampleRepo = new GitSampleRepoRule();






    @Test public void simpleTravisYml() throws Exception {

        sampleRepo.init();
        sampleRepo.write("Jenkinsfile", "simpleBuild {   \n git_repo = \"https://github.com/cloudbeers/PR-demo\" \n script='ls' }");
        sampleRepo.git("add", "Jenkinsfile");
        sampleRepo.git("commit", "--message=files");
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                p.setDefinition(new CpsScmFlowDefinition(new GitStep(sampleRepo.toString()).createSCM(), "Jenkinsfile"));
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                story.j.assertLogContains("Finished: SUCCESS",
                        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b)));


            }
        });
    }

    @Test public void envAndParam() throws Exception {

        sampleRepo.init();
        sampleRepo.write("Jenkinsfile",
        "echo(\"outer this:\"+this.getClass().getName())\n"+
        "if (this instanceof Closure) {\n"+
        "  echo(\"outer owner:\"+owner.getClass().getName())\n"+
        "  echo(\"outer delegate:\"+delegate.getClass().getName())\n"+
        "}\n"+
        "simpleBuild {   \n"+
        "    echo(\"simpleBuild this:\"+this.getClass().getName())\n"+
        "    if (this instanceof Closure) {\n"+
        "        echo(\"simpleBuild owner:\"+owner.getClass().getName())\n"+
        "        echo(\"simpleBuild delegate:\"+delegate.getClass().getName())\n"+
        "    }\n"+
        "    map = [url: \"http://host/path/${PARAM}/${env.BUILD_NUMBER}/tail\"] \n" +
        "    git_repo = \"https://github.com/cloudbeers/PR-demo\" \n"+
        "    script='echo \"url: $url\"' \n"+
        "}");
        sampleRepo.git("add", "Jenkinsfile");
        sampleRepo.git("commit", "--message=files");
        story.addStep(new Statement() {
            @Override public void evaluate() throws Throwable {
                WorkflowJob p = story.j.jenkins.createProject(WorkflowJob.class, "p");
                p.addProperty(new ParametersDefinitionProperty(
                    new StringParameterDefinition("PARAM","value")
                ));
                p.setDefinition(new CpsScmFlowDefinition(new GitStep(sampleRepo.toString()).createSCM(), "Jenkinsfile"));
                WorkflowRun b = p.scheduleBuild2(0).waitForStart();
                story.j.assertLogContains("Finished: SUCCESS",
                        story.j.assertBuildStatusSuccess(story.j.waitForCompletion(b)));
                story.j.assertLogContains("url: http://host/path/value/1/tail",b);
            }
        });
    }

}
