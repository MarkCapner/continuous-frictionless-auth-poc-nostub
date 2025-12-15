package com.poc.api.persistence;

import com.poc.api.model.PolicyRule;
import com.poc.api.model.PolicyScope;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class PolicyRuleRepository {

    private final JdbcTemplate jdbc;

    public PolicyRuleRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<PolicyRule> MAPPER = (rs, rowNum) -> new PolicyRule(
            rs.getLong("id"),
            PolicyScope.valueOf(rs.getString("scope")),
            rs.getString("scope_ref"),
            rs.getInt("priority"),
            rs.getBoolean("enabled"),
            rs.getString("condition_json"),
            rs.getString("action_json"),
            rs.getString("description"),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class)
    );

    public Optional<PolicyRule> findById(long id) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(
                    "SELECT * FROM policy_rule WHERE id = ?",
                    MAPPER,
                    id
            ));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<PolicyRule> listAll() {
        return jdbc.query("SELECT * FROM policy_rule ORDER BY priority DESC, id DESC", MAPPER);
    }

    public List<PolicyRule> listByScope(PolicyScope scope, String scopeRef, boolean enabledOnly) {
        if (scope == PolicyScope.GLOBAL) {
            return enabledOnly
                    ? jdbc.query("SELECT * FROM policy_rule WHERE scope='GLOBAL' AND enabled=TRUE ORDER BY priority DESC, id DESC", MAPPER)
                    : jdbc.query("SELECT * FROM policy_rule WHERE scope='GLOBAL' ORDER BY priority DESC, id DESC", MAPPER);
        }
        if (enabledOnly) {
            return jdbc.query(
                    "SELECT * FROM policy_rule WHERE scope=? AND scope_ref=? AND enabled=TRUE ORDER BY priority DESC, id DESC",
                    MAPPER, scope.name(), scopeRef
            );
        }
        return jdbc.query(
                "SELECT * FROM policy_rule WHERE scope=? AND scope_ref=? ORDER BY priority DESC, id DESC",
                MAPPER, scope.name(), scopeRef
        );
    }

    public long insert(PolicyRule rule) {
        // We purposely avoid KeyHolder usage complexity by using RETURNING.
        return jdbc.queryForObject(
                "INSERT INTO policy_rule(scope, scope_ref, priority, enabled, condition_json, action_json, description) " +
                        "VALUES (?,?,?,?,?::jsonb,?::jsonb,?) RETURNING id",
                Long.class,
                rule.getScope().name(),
                rule.getScopeRef(),
                rule.getPriority(),
                rule.isEnabled(),
                rule.getConditionJson(),
                rule.getActionJson(),
                rule.getDescription()
        );
    }

    public void update(long id, PolicyRule rule) {
        jdbc.update(
                "UPDATE policy_rule SET scope=?, scope_ref=?, priority=?, enabled=?, condition_json=?::jsonb, action_json=?::jsonb, " +
                        "description=?, updated_at=now() WHERE id=?",
                rule.getScope().name(),
                rule.getScopeRef(),
                rule.getPriority(),
                rule.isEnabled(),
                rule.getConditionJson(),
                rule.getActionJson(),
                rule.getDescription(),
                id
        );
    }

    public void setEnabled(long id, boolean enabled) {
        jdbc.update("UPDATE policy_rule SET enabled=?, updated_at=now() WHERE id=?", enabled, id);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM policy_rule WHERE id=?", id);
    }
}
