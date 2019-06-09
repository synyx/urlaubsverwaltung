package org.synyx.urlaubsverwaltung.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("uv.security.ldap")
public class SecurityLdapConfigurationProperties {

    private String url;
    private String base;
    private String managerDn;
    private String managerPassword;
    private String userSearchBase;
    private String userSearchFilter;

    private SecurityLdapSync sync = new SecurityLdapSync();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getManagerDn() {
        return managerDn;
    }

    public void setManagerDn(String managerDn) {
        this.managerDn = managerDn;
    }

    public String getManagerPassword() {
        return managerPassword;
    }

    public void setManagerPassword(String managerPassword) {
        this.managerPassword = managerPassword;
    }

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public void setUserSearchBase(String userSearchBase) {
        this.userSearchBase = userSearchBase;
    }

    public String getUserSearchFilter() {
        return userSearchFilter;
    }

    public void setUserSearchFilter(String userSearchFilter) {
        this.userSearchFilter = userSearchFilter;
    }

    public SecurityLdapSync getSync() {
        return sync;
    }

    public void setSync(SecurityLdapSync sync) {
        this.sync = sync;
    }

    public static class SecurityLdapSync {

        private String userSearchBase;
        private String userDn;
        private String password;

        public String getUserSearchBase() {
            return userSearchBase;
        }

        public void setUserSearchBase(String userSearchBase) {
            this.userSearchBase = userSearchBase;
        }

        public String getUserDn() {
            return userDn;
        }

        public void setUserDn(String userDn) {
            this.userDn = userDn;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}

