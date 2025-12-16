package com.poc.api.admin.dto;

import java.time.OffsetDateTime;

public class PolicyRule {

    private long id;
    private PolicyScope scope;
    private String scopeRef;
    private int priority;
    private boolean enabled;
    private String conditionJson;
    private String actionJson;
    private String description;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public PolicyRule() {}

    public PolicyRule(long id, PolicyScope scope, String scopeRef, int priority, boolean enabled,
                      String conditionJson, String actionJson, String description,
                      OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.scope = scope;
        this.scopeRef = scopeRef;
        this.priority = priority;
        this.enabled = enabled;
        this.conditionJson = conditionJson;
        this.actionJson = actionJson;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public PolicyScope getScope() { return scope; }
    public void setScope(PolicyScope scope) { this.scope = scope; }

    public String getScopeRef() { return scopeRef; }
    public void setScopeRef(String scopeRef) { this.scopeRef = scopeRef; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getConditionJson() { return conditionJson; }
    public void setConditionJson(String conditionJson) { this.conditionJson = conditionJson; }

    public String getActionJson() { return actionJson; }
    public void setActionJson(String actionJson) { this.actionJson = actionJson; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
