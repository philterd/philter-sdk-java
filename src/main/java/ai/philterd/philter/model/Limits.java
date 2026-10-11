/*******************************************************************************
 * Copyright 2026 Philterd, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package ai.philterd.philter.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * The limits and rules Philter enforces, and what the caller may do, from {@code getLimits()}. Read them instead
 * of hard-coding them, so a client stays right when a deployment changes a setting or Philter changes a rule.
 * Fields Philter adds later are ignored.
 */
public class Limits {

    /** Request sizes and paging. */
    public static class Requests {

        @Expose
        @SerializedName("maxDocumentBytes")
        private long maxDocumentBytes;

        @Expose
        @SerializedName("maxBodyBytes")
        private long maxBodyBytes;

        @Expose
        @SerializedName("defaultPageSize")
        private int defaultPageSize;

        @Expose
        @SerializedName("maxPageSize")
        private int maxPageSize;

        /** The largest body {@code filter} and {@code explain} accept, in bytes. */
        public long getMaxDocumentBytes() {
            return maxDocumentBytes;
        }

        public void setMaxDocumentBytes(long maxDocumentBytes) {
            this.maxDocumentBytes = maxDocumentBytes;
        }

        /** The largest body any other POST or PUT accepts, in bytes. */
        public long getMaxBodyBytes() {
            return maxBodyBytes;
        }

        public void setMaxBodyBytes(long maxBodyBytes) {
            this.maxBodyBytes = maxBodyBytes;
        }

        /** The page size a listing uses when no limit is given. */
        public int getDefaultPageSize() {
            return defaultPageSize;
        }

        public void setDefaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
        }

        /** The largest limit a listing honors. A larger limit is reduced to this. */
        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }

    }

    /**
     * The rule for names used in a request path: custom list names, context names, legal hold references, and
     * usernames.
     */
    public static class Names {

        @Expose
        @SerializedName("forbiddenCharacters")
        private List<String> forbiddenCharacters;

        @Expose
        @SerializedName("controlCharactersForbidden")
        private boolean controlCharactersForbidden;

        @Expose
        @SerializedName("reservedNames")
        private List<String> reservedNames;

        @Expose
        @SerializedName("rule")
        private String rule;

        /** The characters such a name cannot contain. */
        public List<String> getForbiddenCharacters() {
            return forbiddenCharacters;
        }

        public void setForbiddenCharacters(List<String> forbiddenCharacters) {
            this.forbiddenCharacters = forbiddenCharacters;
        }

        /** Whether control characters are refused too. */
        public boolean isControlCharactersForbidden() {
            return controlCharactersForbidden;
        }

        public void setControlCharactersForbidden(boolean controlCharactersForbidden) {
            this.controlCharactersForbidden = controlCharactersForbidden;
        }

        /** Whole names that are refused. */
        public List<String> getReservedNames() {
            return reservedNames;
        }

        public void setReservedNames(List<String> reservedNames) {
            this.reservedNames = reservedNames;
        }

        /** The rule in words, as Philter's error messages state it. */
        public String getRule() {
            return rule;
        }

        public void setRule(String rule) {
            this.rule = rule;
        }

    }

    /** Policy names, descriptions, and notes. */
    public static class Policies {

        @Expose
        @SerializedName("nameMaxLength")
        private int nameMaxLength;

        @Expose
        @SerializedName("namePattern")
        private String namePattern;

        @Expose
        @SerializedName("reservedNamePrefix")
        private String reservedNamePrefix;

        @Expose
        @SerializedName("reservedNames")
        private List<String> reservedNames;

        @Expose
        @SerializedName("defaultPolicyName")
        private String defaultPolicyName;

        @Expose
        @SerializedName("descriptionMaxLength")
        private int descriptionMaxLength;

        @Expose
        @SerializedName("notesMaxLength")
        private int notesMaxLength;

        /** The most characters a policy name can have. */
        public int getNameMaxLength() {
            return nameMaxLength;
        }

        public void setNameMaxLength(int nameMaxLength) {
            this.nameMaxLength = nameMaxLength;
        }

        /** The regular expression a policy name must match. */
        public String getNamePattern() {
            return namePattern;
        }

        public void setNamePattern(String namePattern) {
            this.namePattern = namePattern;
        }

        /** The prefix of managed policies' names, which a user's policy cannot start with. */
        public String getReservedNamePrefix() {
            return reservedNamePrefix;
        }

        public void setReservedNamePrefix(String reservedNamePrefix) {
            this.reservedNamePrefix = reservedNamePrefix;
        }

        /** Whole names a policy cannot have, because the API's paths use them for something else. */
        public List<String> getReservedNames() {
            return reservedNames;
        }

        public void setReservedNames(List<String> reservedNames) {
            this.reservedNames = reservedNames;
        }

        /** The policy every user is given, which cannot be deleted. */
        public String getDefaultPolicyName() {
            return defaultPolicyName;
        }

        public void setDefaultPolicyName(String defaultPolicyName) {
            this.defaultPolicyName = defaultPolicyName;
        }

        /** The most characters a policy description can have. */
        public int getDescriptionMaxLength() {
            return descriptionMaxLength;
        }

        public void setDescriptionMaxLength(int descriptionMaxLength) {
            this.descriptionMaxLength = descriptionMaxLength;
        }

        /** The most characters a policy's notes can have. */
        public int getNotesMaxLength() {
            return notesMaxLength;
        }

        public void setNotesMaxLength(int notesMaxLength) {
            this.notesMaxLength = notesMaxLength;
        }

    }

    /** Custom list sizes. */
    public static class CustomLists {

        @Expose
        @SerializedName("maxItems")
        private int maxItems;

        @Expose
        @SerializedName("itemMaxLength")
        private int itemMaxLength;

        /** The most items a custom list can have. */
        public int getMaxItems() {
            return maxItems;
        }

        public void setMaxItems(int maxItems) {
            this.maxItems = maxItems;
        }

        /** The most characters an item can have. */
        public int getItemMaxLength() {
            return itemMaxLength;
        }

        public void setItemMaxLength(int itemMaxLength) {
            this.itemMaxLength = itemMaxLength;
        }

    }

    /** Always-redact and never-redact list sizes. */
    public static class RedactLists {

        @Expose
        @SerializedName("maxTerms")
        private int maxTerms;

        @Expose
        @SerializedName("termMaxLength")
        private int termMaxLength;

        /** The most terms each of the always-redact and never-redact lists can have. */
        public int getMaxTerms() {
            return maxTerms;
        }

        public void setMaxTerms(int maxTerms) {
            this.maxTerms = maxTerms;
        }

        /** The most characters a term can have. */
        public int getTermMaxLength() {
            return termMaxLength;
        }

        public void setTermMaxLength(int termMaxLength) {
            this.termMaxLength = termMaxLength;
        }

    }

    /** Context limits. */
    public static class Contexts {

        @Expose
        @SerializedName("maxPerUser")
        private int maxPerUser;

        @Expose
        @SerializedName("maxEntries")
        private int maxEntries;

        /** The most contexts a user can have. */
        public int getMaxPerUser() {
            return maxPerUser;
        }

        public void setMaxPerUser(int maxPerUser) {
            this.maxPerUser = maxPerUser;
        }

        /** The most entries a context holds. Adding one more evicts the least-read entry. */
        public int getMaxEntries() {
            return maxEntries;
        }

        public void setMaxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
        }

    }

    /** Webhook rules. */
    public static class Webhook {

        @Expose
        @SerializedName("secretMinLength")
        private int secretMinLength;

        /** The fewest characters a webhook secret can have. */
        public int getSecretMinLength() {
            return secretMinLength;
        }

        public void setSecretMinLength(int secretMinLength) {
            this.secretMinLength = secretMinLength;
        }

    }

    /** Legal hold rules. */
    public static class LegalHolds {

        @Expose
        @SerializedName("scopeTypes")
        private List<String> scopeTypes;

        /** The scope types a legal hold can have. */
        public List<String> getScopeTypes() {
            return scopeTypes;
        }

        public void setScopeTypes(List<String> scopeTypes) {
            this.scopeTypes = scopeTypes;
        }

    }

    /** User rules. */
    public static class Users {

        @Expose
        @SerializedName("roles")
        private List<String> roles;

        @Expose
        @SerializedName("reservedUsernames")
        private List<String> reservedUsernames;

        /** The roles a user can have. */
        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        /** Usernames that cannot be used, compared without regard to case. */
        public List<String> getReservedUsernames() {
            return reservedUsernames;
        }

        public void setReservedUsernames(List<String> reservedUsernames) {
            this.reservedUsernames = reservedUsernames;
        }

    }

    /** Audit log export limits. */
    public static class AuditExport {

        @Expose
        @SerializedName("maxWindowDays")
        private int maxWindowDays;

        @Expose
        @SerializedName("defaultPageSize")
        private int defaultPageSize;

        @Expose
        @SerializedName("maxPageSize")
        private int maxPageSize;

        /** The most days between an export's from and to dates. */
        public int getMaxWindowDays() {
            return maxWindowDays;
        }

        public void setMaxWindowDays(int maxWindowDays) {
            this.maxWindowDays = maxWindowDays;
        }

        /** The rows a page of the export has when no limit is given. */
        public int getDefaultPageSize() {
            return defaultPageSize;
        }

        public void setDefaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
        }

        /** The most rows a page of the export can have. */
        public int getMaxPageSize() {
            return maxPageSize;
        }

        public void setMaxPageSize(int maxPageSize) {
            this.maxPageSize = maxPageSize;
        }

    }

    /** How long a session key, the key issued by password sign-in, stays valid. */
    public static class SessionKeys {

        @Expose
        @SerializedName("idleTimeoutMinutes")
        private int idleTimeoutMinutes;

        @Expose
        @SerializedName("maxLifetimeMinutes")
        private int maxLifetimeMinutes;

        /** Minutes without a request after which a session key expires. */
        public int getIdleTimeoutMinutes() {
            return idleTimeoutMinutes;
        }

        public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
            this.idleTimeoutMinutes = idleTimeoutMinutes;
        }

        /** Minutes after issue at which a session key expires, whatever its use. */
        public int getMaxLifetimeMinutes() {
            return maxLifetimeMinutes;
        }

        public void setMaxLifetimeMinutes(int maxLifetimeMinutes) {
            this.maxLifetimeMinutes = maxLifetimeMinutes;
        }

    }

    /** What the caller may do. */
    public static class Caller {

        @Expose
        @SerializedName("role")
        private String role;

        @Expose
        @SerializedName("contextCount")
        private int contextCount;

        @Expose
        @SerializedName("crossUserAccess")
        private boolean crossUserAccess;

        @Expose
        @SerializedName("ledgerDeletion")
        private boolean ledgerDeletion;

        /** The caller's role, read when the request is made. */
        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        /** How many contexts the caller has, to compare with {@link Contexts#getMaxPerUser()}. */
        public int getContextCount() {
            return contextCount;
        }

        public void setContextCount(int contextCount) {
            this.contextCount = contextCount;
        }

        /**
         * Whether the caller may reach other users' resources with {@code owner} and all-users listings: an
         * administrator, with {@code ADMIN_CROSS_USER_ACCESS_ENABLED} on. Always {@code false} for a user who is
         * not an administrator.
         */
        public boolean isCrossUserAccess() {
            return crossUserAccess;
        }

        public void setCrossUserAccess(boolean crossUserAccess) {
            this.crossUserAccess = crossUserAccess;
        }

        /**
         * Whether the caller may delete and purge ledger chains: an administrator, with
         * {@code LEDGER_DELETION_ENABLED} on. Always {@code false} for a user who is not an administrator.
         */
        public boolean isLedgerDeletion() {
            return ledgerDeletion;
        }

        public void setLedgerDeletion(boolean ledgerDeletion) {
            this.ledgerDeletion = ledgerDeletion;
        }

    }

    @Expose
    @SerializedName("requests")
    private Requests requests;

    @Expose
    @SerializedName("password")
    private PasswordRules password;

    @Expose
    @SerializedName("names")
    private Names names;

    @Expose
    @SerializedName("policies")
    private Policies policies;

    @Expose
    @SerializedName("customLists")
    private CustomLists customLists;

    @Expose
    @SerializedName("redactLists")
    private RedactLists redactLists;

    @Expose
    @SerializedName("contexts")
    private Contexts contexts;

    @Expose
    @SerializedName("webhook")
    private Webhook webhook;

    @Expose
    @SerializedName("legalHolds")
    private LegalHolds legalHolds;

    @Expose
    @SerializedName("users")
    private Users users;

    @Expose
    @SerializedName("auditExport")
    private AuditExport auditExport;

    @Expose
    @SerializedName("sessionKeys")
    private SessionKeys sessionKeys;

    @Expose
    @SerializedName("caller")
    private Caller caller;

    /** Request sizes and paging. */
    public Requests getRequests() {
        return requests;
    }

    public void setRequests(Requests requests) {
        this.requests = requests;
    }

    /** The rules a password must meet. */
    public PasswordRules getPassword() {
        return password;
    }

    public void setPassword(PasswordRules password) {
        this.password = password;
    }

    /**
     * The rule for names used in a request path: custom list names, context names, legal hold references, and
     * usernames.
     */
    public Names getNames() {
        return names;
    }

    public void setNames(Names names) {
        this.names = names;
    }

    /** Policy names, descriptions, and notes. */
    public Policies getPolicies() {
        return policies;
    }

    public void setPolicies(Policies policies) {
        this.policies = policies;
    }

    /** Custom list sizes. */
    public CustomLists getCustomLists() {
        return customLists;
    }

    public void setCustomLists(CustomLists customLists) {
        this.customLists = customLists;
    }

    /** Always-redact and never-redact list sizes. */
    public RedactLists getRedactLists() {
        return redactLists;
    }

    public void setRedactLists(RedactLists redactLists) {
        this.redactLists = redactLists;
    }

    /** Context limits. */
    public Contexts getContexts() {
        return contexts;
    }

    public void setContexts(Contexts contexts) {
        this.contexts = contexts;
    }

    /** Webhook rules. */
    public Webhook getWebhook() {
        return webhook;
    }

    public void setWebhook(Webhook webhook) {
        this.webhook = webhook;
    }

    /** Legal hold rules. */
    public LegalHolds getLegalHolds() {
        return legalHolds;
    }

    public void setLegalHolds(LegalHolds legalHolds) {
        this.legalHolds = legalHolds;
    }

    /** User rules. */
    public Users getUsers() {
        return users;
    }

    public void setUsers(Users users) {
        this.users = users;
    }

    /** Audit log export limits. */
    public AuditExport getAuditExport() {
        return auditExport;
    }

    public void setAuditExport(AuditExport auditExport) {
        this.auditExport = auditExport;
    }

    /** How long a session key, the key issued by password sign-in, stays valid. */
    public SessionKeys getSessionKeys() {
        return sessionKeys;
    }

    public void setSessionKeys(SessionKeys sessionKeys) {
        this.sessionKeys = sessionKeys;
    }

    /** What the caller may do. */
    public Caller getCaller() {
        return caller;
    }

    public void setCaller(Caller caller) {
        this.caller = caller;
    }

}
