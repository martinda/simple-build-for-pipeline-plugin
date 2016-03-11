package org.jenkinsci.plugins.simplebuild;

import java.io.Serializable;
import java.util.Map;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

class SimpleBuild implements Serializable {
    private String machine;
    private String docker;
    private String gitRepo;
    private String before_script;
    private String script;
    private String after_script;
    private Map<String,String> map;
    private Map<String,String> notifications;

    @Whitelisted
    public String getMachine() {
        return machine;
    }
    @Whitelisted
    public void setMachine(String machine) {
        this.machine = machine;
    }

    @Whitelisted
    public String getGitRepo() {
        System.out.println("getGitRepo() ->"+gitRepo);
        return gitRepo;
    }
    @Whitelisted
    public void setGitRepo(String gitRepo) {
        System.out.println("setGitRepo("+gitRepo+")");
        this.gitRepo = gitRepo;
    }

    @Whitelisted
    public String getScript() {
        return script;
    }
    @Whitelisted
    public void setScript(String script) {
        this.script = script;
    }

    @Whitelisted
    public Map<String,String> getMap() {
        System.out.println("getMap() ->"+map);
        return map;
    }
    @Whitelisted
    public void setMap(Map<String, String> map) {
        System.out.println("setMap("+map+")");
        this.map = map;
    }

    @Whitelisted
    public Map<String, String> getNotifications() {
        return notifications;
    }
    @Whitelisted
    public void setNotifications(Map<String, String> notifications) {
        this.notifications = notifications;
    }
    
}
