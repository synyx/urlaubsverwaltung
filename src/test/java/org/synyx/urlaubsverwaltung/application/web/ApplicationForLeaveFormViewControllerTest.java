package org.synyx.urlaubsverwaltung.application.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.Errors;
import org.synyx.urlaubsverwaltung.account.Account;
import org.synyx.urlaubsverwaltung.account.AccountService;
import org.synyx.urlaubsverwaltung.application.ApplicationSettings;
import org.synyx.urlaubsverwaltung.application.domain.Application;
import org.synyx.urlaubsverwaltung.application.domain.VacationType;
import org.synyx.urlaubsverwaltung.application.service.ApplicationInteractionService;
import org.synyx.urlaubsverwaltung.application.service.EditApplicationForLeaveNotAllowedException;
import org.synyx.urlaubsverwaltung.application.service.VacationTypeService;
import org.synyx.urlaubsverwaltung.overtime.OvertimeSettings;
import org.synyx.urlaubsverwaltung.period.DayLength;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.person.Role;
import org.synyx.urlaubsverwaltung.person.UnknownPersonException;
import org.synyx.urlaubsverwaltung.settings.Settings;
import org.synyx.urlaubsverwaltung.settings.SettingsService;
import org.synyx.urlaubsverwaltung.web.DateFormatAware;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.math.BigDecimal.TEN;
import static java.time.Month.DECEMBER;
import static java.time.Month.JANUARY;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.ALLOWED;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.WAITING;
import static org.synyx.urlaubsverwaltung.application.domain.VacationCategory.OVERTIME;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;

@ExtendWith(MockitoExtension.class)
class ApplicationForLeaveFormViewControllerTest {

    private ApplicationForLeaveFormViewController sut;

    @Mock
    private PersonService personService;
    @Mock
    private AccountService accountService;
    @Mock
    private VacationTypeService vacationTypeService;
    @Mock
    private ApplicationInteractionService applicationInteractionService;
    @Mock
    private ApplicationForLeaveFormValidator applicationForLeaveFormValidator;
    @Mock
    private SettingsService settingsService;

    private final DateFormatAware dateFormatAware = new DateFormatAware();

    private static final int PERSON_ID = 1;
    private final Clock clock = Clock.systemUTC();

    @BeforeEach
    void setUp() {
        sut = new ApplicationForLeaveFormViewController(personService, accountService, vacationTypeService,
            applicationInteractionService, applicationForLeaveFormValidator, settingsService, dateFormatAware, clock);
    }

    @Test
    void overtimeIsActivated() throws Exception {

        final Person person = new Person();
        when(personService.getSignedInUser()).thenReturn(person);

        final int year = Year.now(clock).getValue();
        final LocalDate validFrom = LocalDate.of(2014, JANUARY, 1);
        final LocalDate validTo = LocalDate.of(2014, DECEMBER, 31);
        final Account account = new Account(person, validFrom, validTo, TEN, TEN, TEN, "comment");
        when(accountService.getHolidaysAccount(year, person)).thenReturn(Optional.of(account));

        final VacationType vacationType = new VacationType();
        when(vacationTypeService.getVacationTypes()).thenReturn(singletonList(vacationType));

        final OvertimeSettings overtimeSettings = new OvertimeSettings();
        overtimeSettings.setOvertimeActive(true);

        final Settings settings = new Settings();
        settings.setOvertimeSettings(overtimeSettings);
        when(settingsService.getSettings()).thenReturn(settings);

        final ResultActions resultActions = perform(get("/web/application/new"));
        resultActions.andExpect(status().isOk());
        resultActions.andExpect(model().attribute("overtimeActive", is(true)));
        resultActions.andExpect(model().attribute("vacationTypes", hasItems(vacationType)));
        resultActions.andExpect(model().attribute("showHalfDayOption", is(true)));
    }

    @Test
    void overtimeIsDeactivated() throws Exception {

        final Person person = new Person();
        when(personService.getSignedInUser()).thenReturn(person);

        final int year = Year.now(clock).getValue();
        final LocalDate validFrom = LocalDate.of(2014, JANUARY, 1);
        final LocalDate validTo = LocalDate.of(2014, DECEMBER, 31);
        final Account account = new Account(person, validFrom, validTo, TEN, TEN, TEN, "comment");
        when(accountService.getHolidaysAccount(year, person)).thenReturn(Optional.of(account));

        final VacationType vacationType = new VacationType();
        when(vacationTypeService.getVacationTypesFilteredBy(OVERTIME)).thenReturn(singletonList(vacationType));

        final OvertimeSettings overtimeSettings = new OvertimeSettings();
        overtimeSettings.setOvertimeActive(false);

        final Settings settings = new Settings();
        settings.setOvertimeSettings(overtimeSettings);
        when(settingsService.getSettings()).thenReturn(settings);

        final ResultActions resultActions = perform(get("/web/application/new"));
        resultActions.andExpect(status().isOk());
        resultActions.andExpect(model().attribute("overtimeActive", is(false)));
        resultActions.andExpect(model().attribute("vacationTypes", hasItems(vacationType)));
        resultActions.andExpect(model().attribute("showHalfDayOption", is(true)));
    }

    @Test
    void halfdayIsDeactivated() throws Exception {

        final Person person = new Person();
        when(personService.getSignedInUser()).thenReturn(person);

        final int year = Year.now(clock).getValue();
        final LocalDate validFrom = LocalDate.of(2014, JANUARY, 1);
        final LocalDate validTo = LocalDate.of(2014, DECEMBER, 31);
        final Account account = new Account(person, validFrom, validTo, TEN, TEN, TEN, "comment");
        when(accountService.getHolidaysAccount(year, person)).thenReturn(Optional.of(account));

        final VacationType vacationType = new VacationType();
        when(vacationTypeService.getVacationTypesFilteredBy(OVERTIME)).thenReturn(singletonList(vacationType));

        final OvertimeSettings overtimeSettings = new OvertimeSettings();
        overtimeSettings.setOvertimeActive(false);

        final Settings settings = new Settings();
        settings.setOvertimeSettings(overtimeSettings);
        final var appSettings = new ApplicationSettings();
        appSettings.setAllowHalfDays(false);
        settings.setApplicationSettings(appSettings);
        when(settingsService.getSettings()).thenReturn(settings);

        final ResultActions resultActions = perform(get("/web/application/new"));
        resultActions.andExpect(status().isOk());
        resultActions.andExpect(model().attribute("overtimeActive", is(false)));
        resultActions.andExpect(model().attribute("vacationTypes", hasItems(vacationType)));
        resultActions.andExpect(model().attribute("showHalfDayOption", is(false)));
    }

    @Test
    void getNewApplicationFormDefaultsToSignedInPersonIfPersonIdNotGiven() throws Exception {

        final Person signedInPerson = new Person();
        when(personService.getSignedInUser()).thenReturn(signedInPerson);

        final LocalDate validFrom = LocalDate.of(2014, JANUARY, 1);
        final LocalDate validTo = LocalDate.of(2014, DECEMBER, 31);
        final Account account = new Account(signedInPerson, validFrom, validTo, TEN, TEN, TEN, "comment");
        when(accountService.getHolidaysAccount(anyInt(), eq(signedInPerson))).thenReturn(Optional.of(account));
        when(settingsService.getSettings()).thenReturn(new Settings());

        perform(get("/web/application/new"))
            .andExpect(model().attribute("person", signedInPerson));
    }

    @Test
    void getNewApplicationFormUsesPersonOfGivenPersonId() throws Exception {

        when(personService.getSignedInUser()).thenReturn(personWithRole(OFFICE));

        final Person person = new Person();
        when(personService.getPersonByID(PERSON_ID)).thenReturn(Optional.of(person));

        final LocalDate validFrom = LocalDate.of(2014, JANUARY, 1);
        final LocalDate validTo = LocalDate.of(2014, DECEMBER, 31);
        final Account account = new Account(person, validFrom, validTo, TEN, TEN, TEN, "comment");
        when(accountService.getHolidaysAccount(anyInt(), eq(person))).thenReturn(Optional.of(account));
        when(settingsService.getSettings()).thenReturn(new Settings());

        perform(get("/web/application/new")
            .param("person", Integer.toString(PERSON_ID)))
            .andExpect(model().attribute("person", person));
    }

    @Test
    void getNewApplicationFormForUnknownPersonIdThrowsUnknownPersonException() {

        when(personService.getPersonByID(PERSON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            perform(get("/web/application/new")
                .param("person", Integer.toString(PERSON_ID)))
        ).hasCauseInstanceOf(UnknownPersonException.class);
    }

    @Test
    void getNewApplicationFormThrowsAccessDeniedExceptionIfGivenPersonNotSignedInPersonAndNotOffice() {

        final Person signedInPerson = new Person();
        when(personService.getSignedInUser()).thenReturn(signedInPerson);

        final Person person = personWithId(PERSON_ID);
        when(personService.getPersonByID(PERSON_ID)).thenReturn(Optional.of(person));

        assertThatThrownBy(() ->
            perform(get("/web/application/new")
                .param("person", Integer.toString(PERSON_ID)))
        ).hasCauseInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getNewApplicationFormAccessibleIfGivenPersonIsSignedInPerson() throws Exception {

        final Person signedInPerson = new Person();
        when(personService.getSignedInUser()).thenReturn(signedInPerson);
        when(personService.getPersonByID(PERSON_ID)).thenReturn(Optional.of(signedInPerson));

        perform(get("/web/application/new")
            .param("person", Integer.toString(PERSON_ID)))
            .andExpect(status().isOk());
    }

    @Test
    void getNewApplicationFormAccessibleForOfficeIfGivenPersonNotSignedInPerson() throws Exception {

        when(personService.getSignedInUser()).thenReturn(personWithRole(OFFICE));
        when(personService.getPersonByID(PERSON_ID)).thenReturn(Optional.of(personWithId(PERSON_ID)));

        perform(get("/web/application/new")
            .param("person", Integer.toString(PERSON_ID)))
            .andExpect(status().isOk());
    }

    @Test
    void getNewApplicationFormWithGivenDateFrom() throws Exception {

        when(personService.getSignedInUser()).thenReturn(personWithRole(OFFICE));

        final Person person = new Person();
        when(personService.getPersonByID(PERSON_ID)).thenReturn(Optional.of(person));

        final LocalDate validFrom = LocalDate.of(2014, JANUARY, 1);
        final LocalDate validTo = LocalDate.of(2014, DECEMBER, 31);
        final Account account = new Account(person, validFrom, validTo, TEN, TEN, TEN, "comment");
        when(accountService.getHolidaysAccount(anyInt(), eq(person))).thenReturn(Optional.of(account));
        when(settingsService.getSettings()).thenReturn(new Settings());

        final ResultActions resultActions = perform(get("/web/application/new")
            .param("person", Integer.toString(PERSON_ID))
            .param("from", "2020-10-30"));

        resultActions
            .andExpect(model().attribute("person", person))
            .andExpect(model().attribute("showHalfDayOption", is(true)))
            .andExpect(model().attribute("application", allOf(
                hasProperty("startDate", is(LocalDate.parse("2020-10-30"))),
                hasProperty("startDateIsoValue", is("2020-10-30")),
                hasProperty("endDate", is(LocalDate.parse("2020-10-30"))),
                hasProperty("endDateIsoValue", is("2020-10-30"))
            )));
    }

    private static Stream<Arguments> datePeriodParams() {
        return Stream.of(
            Arguments.of("27.10.2020", "2020-10-27", "30.10.2020", "2020-10-30"),
            Arguments.of("2020-10-27", "2020-10-27", "2020-10-30", "2020-10-30")
        );
    }

    @ParameterizedTest
    @MethodSource("datePeriodParams")
    void getNewApplicationFormWithGivenDateFromAndDateTo(
        String givenFromString, String expectedFromString, String givenToString, String expectedToString) throws Exception {

        when(personService.getSignedInUser()).thenReturn(personWithRole(OFFICE));

        final Person person = new Person();
        when(personService.getPersonByID(PERSON_ID)).thenReturn(Optional.of(person));

        final LocalDate validFrom = LocalDate.of(2014, JANUARY, 1);
        final LocalDate validTo = LocalDate.of(2014, DECEMBER, 31);
        final Account account = new Account(person, validFrom, validTo, TEN, TEN, TEN, "comment");
        when(accountService.getHolidaysAccount(anyInt(), eq(person))).thenReturn(Optional.of(account));
        when(settingsService.getSettings()).thenReturn(new Settings());

        final ResultActions resultActions = perform(get("/web/application/new")
            .param("person", Integer.toString(PERSON_ID))
            .param("from", givenFromString)
            .param("to", givenToString));

        resultActions
            .andExpect(model().attribute("person", person))
            .andExpect(model().attribute("showHalfDayOption", is(true)))
            .andExpect(model().attribute("application", allOf(
                hasProperty("startDate", is(LocalDate.parse(expectedFromString))),
                hasProperty("startDateIsoValue", is(expectedFromString)),
                hasProperty("endDate", is(LocalDate.parse(expectedToString))),
                hasProperty("endDateIsoValue", is(expectedToString))
            )));
    }

    @Test
    void getNewApplicationFormShowsForm() throws Exception {

        final Person person = new Person();
        when(personService.getSignedInUser()).thenReturn(person);

        perform(get("/web/application/new"))
            .andExpect(view().name("application/app_form"));
    }

    @Test
    void postNewApplicationFormShowFormIfValidationFails() throws Exception {

        when(settingsService.getSettings()).thenReturn(new Settings());

        doAnswer(invocation -> {
            Errors errors = invocation.getArgument(1);
            errors.rejectValue("reason", "errors");
            errors.reject("globalErrors");
            return null;
        }).when(applicationForLeaveFormValidator).validate(any(), any());

        perform(post("/web/application"))
            .andExpect(model().attribute("errors", instanceOf(Errors.class)))
            .andExpect(view().name("application/app_form"))
            .andExpect(model().attribute("showHalfDayOption", is(true)));
    }

    @Test
    void postNewApplicationFormCallsServiceToApplyApplication() throws Exception {

        final Person person = personWithRole(OFFICE);
        when(personService.getSignedInUser()).thenReturn(person);
        when(applicationInteractionService.apply(any(), any(), any())).thenReturn(someApplication());

        perform(post("/web/application")
            .param("vacationType.category", "HOLIDAY"));

        verify(applicationInteractionService).apply(any(), eq(person), any());
    }

    @Test
    void postNewApplicationAddsFlashAttributeAndRedirectsToNewApplication() throws Exception {

        final int applicationId = 11;
        final Person person = personWithRole(OFFICE);

        when(personService.getSignedInUser()).thenReturn(person);
        when(applicationInteractionService.apply(any(), any(), any())).thenReturn(applicationWithId(applicationId));

        perform(post("/web/application")
            .param("vacationType.category", "HOLIDAY"))
            .andExpect(flash().attribute("applySuccess", true))
            .andExpect(status().isFound())
            .andExpect(redirectedUrl("/web/application/" + applicationId));
    }

    @Test
    void editApplicationForm() throws Exception {

        final Person person = new Person();
        when(personService.getSignedInUser()).thenReturn(person);
        when(personService.getActivePersons()).thenReturn(List.of(person));

        final int year = Year.now(clock).getValue();
        final LocalDate validFrom = LocalDate.of(2014, JANUARY, 1);
        final LocalDate validTo = LocalDate.of(2014, DECEMBER, 31);
        final Account account = new Account(person, validFrom, validTo, TEN, TEN, TEN, "comment");
        when(accountService.getHolidaysAccount(year, person)).thenReturn(Optional.of(account));

        final Settings settings = new Settings();
        when(settingsService.getSettings()).thenReturn(settings);

        final VacationType vacationType = new VacationType();
        when(vacationTypeService.getVacationTypes()).thenReturn(singletonList(vacationType));

        final Integer applicationId = 1;
        final Application application = new Application();
        application.setId(applicationId);
        application.setStatus(WAITING);
        when(applicationInteractionService.get(applicationId)).thenReturn(Optional.of(application));

        perform(get("/web/application/1/edit"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("noHolidaysAccount", is(false)))
            .andExpect(view().name("application/app_form"))
            .andExpect(model().attribute("showHalfDayOption", is(true)));
    }

    @Test
    void editApplicationFormHalfdayIsDeactivated() throws Exception {

        final Person person = new Person();
        when(personService.getSignedInUser()).thenReturn(person);
        when(personService.getActivePersons()).thenReturn(List.of(person));

        final int year = Year.now(clock).getValue();
        final LocalDate validFrom = LocalDate.of(2014, JANUARY, 1);
        final LocalDate validTo = LocalDate.of(2014, DECEMBER, 31);
        final Account account = new Account(person, validFrom, validTo, TEN, TEN, TEN, "comment");
        when(accountService.getHolidaysAccount(year, person)).thenReturn(Optional.of(account));

        final Settings settings = new Settings();
        final var appSettings = new ApplicationSettings();
        appSettings.setAllowHalfDays(false);
        settings.setApplicationSettings(appSettings);
        when(settingsService.getSettings()).thenReturn(settings);

        final VacationType vacationType = new VacationType();
        when(vacationTypeService.getVacationTypes()).thenReturn(singletonList(vacationType));

        final Integer applicationId = 1;
        final Application application = new Application();
        application.setId(applicationId);
        application.setStatus(WAITING);
        when(applicationInteractionService.get(applicationId)).thenReturn(Optional.of(application));

        perform(get("/web/application/1/edit"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("noHolidaysAccount", is(false)))
            .andExpect(view().name("application/app_form"))
            .andExpect(model().attribute("showHalfDayOption", is(false)));
    }

    @Test
    void editApplicationFormWithHalfday() throws Exception {

        final Person person = new Person();
        when(personService.getSignedInUser()).thenReturn(person);
        when(personService.getActivePersons()).thenReturn(List.of(person));

        final int year = Year.now(clock).getValue();
        final LocalDate validFrom = LocalDate.of(2014, JANUARY, 1);
        final LocalDate validTo = LocalDate.of(2014, DECEMBER, 31);
        final Account account = new Account(person, validFrom, validTo, TEN, TEN, TEN, "comment");
        when(accountService.getHolidaysAccount(year, person)).thenReturn(Optional.of(account));

        final Settings settings = new Settings();
        final var appSettings = new ApplicationSettings();
        appSettings.setAllowHalfDays(false);
        settings.setApplicationSettings(appSettings);
        when(settingsService.getSettings()).thenReturn(settings);

        final VacationType vacationType = new VacationType();
        when(vacationTypeService.getVacationTypes()).thenReturn(singletonList(vacationType));

        final Integer applicationId = 1;
        final Application application = new Application();
        application.setId(applicationId);
        application.setStatus(WAITING);
        application.setDayLength(DayLength.MORNING);
        when(applicationInteractionService.get(applicationId)).thenReturn(Optional.of(application));

        perform(get("/web/application/1/edit"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("noHolidaysAccount", is(false)))
            .andExpect(view().name("application/app_form"))
            .andExpect(model().attribute("showHalfDayOption", is(true)));
    }


    @Test
    void editApplicationFormUnknownApplication() {

        when(applicationInteractionService.get(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            perform(get("/web/application/1/edit"))
        ).hasCauseInstanceOf(UnknownApplicationForLeaveException.class);
    }

    @Test
    void editApplicationFormNotWaiting() throws Exception {

        when(applicationInteractionService.get(1)).thenReturn(Optional.empty());

        final Integer applicationId = 1;
        final Application application = new Application();
        application.setId(applicationId);
        application.setStatus(ALLOWED);
        when(applicationInteractionService.get(applicationId)).thenReturn(Optional.of(application));

        perform(get("/web/application/1/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("application/app_notwaiting"));
    }

    @Test
    void editApplicationFormNoHolidayAccount() throws Exception {

        final Person person = new Person();
        when(personService.getSignedInUser()).thenReturn(person);

        final int year = Year.now(clock).getValue();
        when(accountService.getHolidaysAccount(year, person)).thenReturn(Optional.empty());

        final Integer applicationId = 1;
        final Application application = new Application();
        application.setId(applicationId);
        application.setStatus(WAITING);
        when(applicationInteractionService.get(applicationId)).thenReturn(Optional.of(application));

        perform(get("/web/application/1/edit"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("noHolidaysAccount", is(true)))
            .andExpect(view().name("application/app_form"));
    }

    @Test
    void sendEditApplicationForm() throws Exception {

        final Person person = new Person();
        when(personService.getSignedInUser()).thenReturn(person);

        final Integer applicationId = 1;
        final Application application = new Application();
        application.setStatus(WAITING);

        final Application editedApplication = new Application();
        editedApplication.setId(applicationId);
        when(applicationInteractionService.get(applicationId)).thenReturn(Optional.of(application));
        when(applicationInteractionService.edit(eq(application), any(Application.class), eq(person), eq(Optional.of("comment")))).thenReturn(editedApplication);

        perform(post("/web/application/1")
            .param("person.id", "1")
            .param("startDate", "28.10.2020")
            .param("endDate", "28.10.2020")
            .param("vacationType.category", "HOLIDAY")
            .param("dayLength", "FULL")
            .param("comment", "comment"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/web/application/1"));
    }

    @Test
    void sendEditApplicationFormIsNotWaiting() throws Exception {

        final Integer applicationId = 1;
        final Application application = new Application();
        application.setId(applicationId);
        application.setStatus(ALLOWED);
        when(applicationInteractionService.get(applicationId)).thenReturn(Optional.of(application));

        perform(post("/web/application/1")
            .param("person.id", "1")
            .param("startDate", "28.10.2020")
            .param("endDate", "28.10.2020")
            .param("vacationType.category", "HOLIDAY")
            .param("dayLength", "FULL")
            .param("comment", "comment"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/web/application/1"))
            .andExpect(flash().attribute("editError", true));
    }

    @Test
    void sendEditApplicationFormApplicationNotFound() {

        final Integer applicationId = 1;
        final Application application = new Application();
        application.setId(applicationId);
        application.setStatus(WAITING);
        when(applicationInteractionService.get(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            perform(post("/web/application/1")
                .param("person.id", "1")
                .param("startDate", "28.10.2020")
                .param("endDate", "28.10.2020")
                .param("vacationType.category", "HOLIDAY")
                .param("dayLength", "FULL")
                .param("comment", "comment"))
        ).hasCauseInstanceOf(UnknownApplicationForLeaveException.class);
    }

    @Test
    void sendEditApplicationFormCannotBeEdited() throws Exception {

        final Person person = new Person();
        when(personService.getSignedInUser()).thenReturn(person);

        final Integer applicationId = 1;
        final Application application = new Application();
        application.setStatus(WAITING);
        final Application editedApplication = new Application();
        editedApplication.setId(applicationId);
        when(applicationInteractionService.get(applicationId)).thenReturn(Optional.of(application));
        when(applicationInteractionService.edit(eq(application), any(Application.class), eq(person), eq(Optional.of("comment")))).thenThrow(EditApplicationForLeaveNotAllowedException.class);

        perform(post("/web/application/1")
            .param("person.id", "1")
            .param("startDate", "28.10.2020")
            .param("endDate", "28.10.2020")
            .param("vacationType.category", "HOLIDAY")
            .param("dayLength", "FULL")
            .param("comment", "comment"))
            .andExpect(status().isOk())
            .andExpect(view().name("application/app_notwaiting"));
    }

    @Test
    void sendEditApplicationFormHasErrors() throws Exception {

        final Integer applicationId = 1;
        final Application application = new Application();
        application.setId(applicationId);
        application.setStatus(WAITING);
        when(applicationInteractionService.get(applicationId)).thenReturn(Optional.of(application));

        final Settings settings = new Settings();
        when(settingsService.getSettings()).thenReturn(settings);

        doAnswer(invocation -> {
            final Errors errors = invocation.getArgument(1);
            errors.rejectValue("reason", "errors");
            errors.reject("globalErrors");
            return null;
        }).when(applicationForLeaveFormValidator).validate(any(), any());

        perform(post("/web/application/1")
            .param("person.id", "1")
            .param("startDate", "28.10.2020")
            .param("endDate", "28.10.2020")
            .param("vacationType.category", "HOLIDAY")
            .param("dayLength", "FULL")
            .param("comment", "comment"))
            .andExpect(status().isOk())
            .andExpect(view().name("application/app_form"))
            .andExpect(model().attribute("showHalfDayOption", is(true)));
    }

    private Person personWithRole(Role role) {
        Person person = new Person();
        person.setPermissions(List.of(role));

        return person;
    }

    private Person personWithId(int id) {
        Person person = new Person();
        person.setId(id);

        return person;
    }

    private Application someApplication() {
        Application application = new Application();
        application.setStartDate(LocalDate.now().plusDays(10));
        application.setEndDate(LocalDate.now().plusDays(20));

        return new Application();
    }

    private Application applicationWithId(int id) {
        Application application = someApplication();
        application.setId(id);

        return application;
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        return standaloneSetup(sut).build().perform(builder);
    }
}
