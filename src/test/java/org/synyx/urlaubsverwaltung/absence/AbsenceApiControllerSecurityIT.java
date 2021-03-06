package org.synyx.urlaubsverwaltung.absence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.synyx.urlaubsverwaltung.TestContainersBase;
import org.synyx.urlaubsverwaltung.department.Department;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.synyx.urlaubsverwaltung.person.Role.DEPARTMENT_HEAD;

@SpringBootTest
class AbsenceApiControllerSecurityIT extends TestContainersBase {

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private PersonService personService;

    @MockBean
    private DepartmentService departmentService;

    @MockBean
    private AbsenceService absenceService;

    @Test
    void getAbsencesWithoutBasicAuthIsUnauthorized() throws Exception {
        final ResultActions resultActions = perform(get("/api/absences"));
        resultActions.andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void getAbsencesAsAuthenticatedUserForOtherUserIsForbidden() throws Exception {
        perform(get("/api/persons/1/absences")
            .param("from", "2016-01-01")
            .param("to", "2016-12-31"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DEPARTMENT_HEAD", username = "departmentHead")
    void getAbsencesAsDepartmentHeadUserForOtherUserIsForbidden() throws Exception {
        final Person person = new Person();
        when(personService.getPersonByID(1)).thenReturn(Optional.of(person));

        final Person departmentHead = new Person();
        departmentHead.setPermissions(List.of(DEPARTMENT_HEAD));
        when(personService.getPersonByUsername("departmentHead")).thenReturn(Optional.of(departmentHead));

        final Department department = new Department();
        department.setMembers(List.of());

        final List<Department> departments = List.of(department);
        when(departmentService.getManagedDepartmentsOfDepartmentHead(departmentHead)).thenReturn(departments);

        perform(get("/api/persons/1/absences")
            .param("from", "2016-01-01")
            .param("to", "2016-12-31"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "DEPARTMENT_HEAD", username = "departmentHead")
    void getAbsencesAsDepartmentHeadUserForOtherUserInSameDepartmentIsOk() throws Exception {
        final Person person = new Person();
        when(personService.getPersonByID(1)).thenReturn(Optional.of(person));

        final Person departmentHead = new Person();
        departmentHead.setPermissions(List.of(DEPARTMENT_HEAD));
        when(personService.getPersonByUsername("departmentHead")).thenReturn(Optional.of(departmentHead));

        final Department department = new Department();
        department.setMembers(List.of(person));
        final List<Department> departments = List.of(department);
        when(departmentService.getManagedDepartmentsOfDepartmentHead(departmentHead)).thenReturn(departments);

        when(absenceService.getOpenAbsences(person, LocalDate.of(2016, Month.JANUARY, 1), LocalDate.of(2016, Month.DECEMBER, 31)))
            .thenReturn(emptyList());

        perform(get("/api/persons/1/absences")
            .param("from", "2016-01-01")
            .param("to", "2016-12-31"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "SECOND_STAGE_AUTHORITY")
    void getAbsencesAsSecondStageAuthorityUserForOtherUserIsForbidden() throws Exception {
        perform(get("/api/persons/1/absences")
            .param("from", "2016-01-01")
            .param("to", "2016-12-31"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ADMIN")
    void getAbsencesAsAdminUserForOtherUserIsForbidden() throws Exception {
        perform(get("/api/persons/1/absences")
            .param("from", "2016-01-01")
            .param("to", "2016-12-31"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "INACTIVE")
    void getAbsencesAsInactiveUserForOtherUserIsForbidden() throws Exception {
        perform(get("/api/persons/1/absences")
            .param("from", "2016-01-01")
            .param("to", "2016-12-31"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "OFFICE")
    void getAbsencesAsOfficeUserForOtherUserIsOk() throws Exception {

        testWithUserWithCorrectPermissions();
    }

    @Test
    @WithMockUser(authorities = "BOSS")
    void getAbsencesAsBossUserForOtherUserIsOk() throws Exception {

        testWithUserWithCorrectPermissions();
    }

    @Test
    @WithMockUser(username = "user")
    void getAbsencesAsOfficeUserForSameUserIsOk() throws Exception {

        final Person person = new Person();
        person.setUsername("user");
        when(personService.getPersonByID(1)).thenReturn(Optional.of(person));

        when(absenceService.getOpenAbsences(person, LocalDate.of(2016, Month.JANUARY, 1), LocalDate.of(2016, Month.DECEMBER, 31)))
            .thenReturn(emptyList());

        perform(get("/api/persons/1/absences")
            .param("from", "2016-01-01")
            .param("to", "2016-12-31"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "differentUser")
    void getAbsencesAsOfficeUserForDifferentUserIsForbidden() throws Exception {

        final Person person = new Person();
        person.setUsername("user");
        when(personService.getPersonByID(1)).thenReturn(Optional.of(person));

        perform(get("/api/persons/1/absences")
            .param("from", "2016-01-01")
            .param("to", "2016-12-31"))
            .andExpect(status().isForbidden());
    }

    private void testWithUserWithCorrectPermissions() throws Exception {
        final Person person = new Person();
        when(personService.getPersonByID(1)).thenReturn(Optional.of(person));

        when(absenceService.getOpenAbsences(person, LocalDate.of(2016, Month.JANUARY, 1), LocalDate.of(2016, Month.DECEMBER, 31)))
            .thenReturn(emptyList());

        perform(get("/api/persons/1/absences")
            .param("from", "2016-01-01")
            .param("to", "2016-12-31"))
            .andExpect(status().isOk());
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build().perform(builder);
    }
}
