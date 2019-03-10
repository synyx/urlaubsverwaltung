package org.synyx.urlaubsverwaltung.security;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.ldap.core.LdapTemplate;


/**
 * @author  Aljona Murygina - murygina@synyx.de
 */
public class LdapUserServiceImplTest {

    private LdapTemplate ldapTemplate;

    private LdapUserService ldapUserService;
    private LdapUserMapper ldapUserMapper;

    @Before
    public void setUp() {

        ldapTemplate = Mockito.mock(LdapTemplate.class);
        ldapUserMapper = Mockito.mock(LdapUserMapper.class);
        ldapUserService = new LdapUserServiceImpl(ldapTemplate, ldapUserMapper, "objectClassFilter", "memberOfFilter");
    }


    @Test
    public void ensureUsesLdapTemplateToFetchUsers() {

        ldapUserService.getLdapUsers();

        Mockito.verify(ldapTemplate).search(Mockito.any(), Mockito.eq(ldapUserMapper));
    }
}
