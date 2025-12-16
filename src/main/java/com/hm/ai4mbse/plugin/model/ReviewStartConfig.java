package com.hm.ai4mbse.plugin.model;

import java.util.HashMap;
import java.util.Map;

public class ReviewStartConfig {
    private String useCase;
    private String ruleFile;
    private String systemElementScope;
    private String projectXml;
    private Map<String, String> parameters = new HashMap<>();

    public String getUseCase() { return useCase; }
    public void setUseCase(String useCase) { this.useCase = useCase; }
    public String getRuleFile() { return ruleFile; }
    public void setRuleFile(String ruleFile) { this.ruleFile = ruleFile; }
    public String getSystemElementScope() { return systemElementScope; }
    public void setSystemElementScope(String systemElementScope) { this.systemElementScope = systemElementScope; }
    public String getProjectXml() { return projectXml; }
    public void setProjectXml(String projectXml) { this.projectXml = projectXml; }
    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }
}