package org.synyx.urlaubsverwaltung.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("uv.security.active-directory")
public class SecurityActiveDirectoryConfigurationProperties {

    private String domain;
    private String url;

    private SecurityActiveDirectorySync sync = new SecurityActiveDirectorySync();

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public SecurityActiveDirectorySync getSync() {
        return sync;
    }

    public void setSync(SecurityActiveDirectorySync sync) {
        this.sync = sync;
    }

    public static class SecurityActiveDirectorySync {

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

