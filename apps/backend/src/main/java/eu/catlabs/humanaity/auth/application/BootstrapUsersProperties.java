package eu.catlabs.humanaity.auth.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "humanaity.auth.bootstrap")
public class BootstrapUsersProperties {

    private List<BootstrapUser> users = new ArrayList<>();

    public List<BootstrapUser> getUsers() {
        return users;
    }

    public void setUsers(List<BootstrapUser> users) {
        this.users = users == null ? new ArrayList<>() : users;
    }

    public static class BootstrapUser {
        private String email;
        private String password;
        private Set<String> roles = new LinkedHashSet<>(Set.of("ROLE_USER"));

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Set<String> getRoles() {
            return roles;
        }

        public void setRoles(Set<String> roles) {
            this.roles = roles == null ? new LinkedHashSet<>(Set.of("ROLE_USER")) : new LinkedHashSet<>(roles);
        }
    }
}
