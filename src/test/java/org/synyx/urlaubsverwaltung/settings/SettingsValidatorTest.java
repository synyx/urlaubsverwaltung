package org.synyx.urlaubsverwaltung.settings;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.Errors;
import org.synyx.urlaubsverwaltung.absence.TimeSettings;
import org.synyx.urlaubsverwaltung.account.AccountSettings;
import org.synyx.urlaubsverwaltung.application.ApplicationSettings;
import org.synyx.urlaubsverwaltung.calendarintegration.ExchangeCalendarSettings;
import org.synyx.urlaubsverwaltung.calendarintegration.GoogleCalendarSettings;
import org.synyx.urlaubsverwaltung.calendarintegration.providers.exchange.ExchangeCalendarProvider;
import org.synyx.urlaubsverwaltung.calendarintegration.providers.google.GoogleCalendarSyncProvider;
import org.synyx.urlaubsverwaltung.overtime.OvertimeSettings;
import org.synyx.urlaubsverwaltung.sicknote.SickNoteSettings;
import org.synyx.urlaubsverwaltung.workingtime.settings.WorkingTimeSettingsEmbeddable;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.synyx.urlaubsverwaltung.period.DayLength.ZERO;

class SettingsValidatorTest {

    private SettingsValidator settingsValidator;

    @BeforeEach
    void setUp() {
        settingsValidator = new SettingsValidator();
    }

    @Test
    void ensureSettingsClassIsSupported() {
        assertThat(settingsValidator.supports(Settings.class)).isTrue();
    }

    @Test
    void ensureOtherClassThanSettingsIsNotSupported() {
        assertThat(settingsValidator.supports(Object.class)).isFalse();
    }

    @Test
    void ensureThatValidateFailsWithOtherClassThanSettings() {
        assertThatIllegalArgumentException().isThrownBy(() -> settingsValidator.validate(new Object(), mock(Errors.class)));
    }

    // Working time settings: Public holidays --------------------------------------------------------------------------
    @Test
    void ensureWorkingTimeSettingsCanNotBeNull() {

        Settings settings = new Settings();
        WorkingTimeSettingsEmbeddable workingTimeSettingsEmbeddable = settings.getWorkingTimeSettings();
        workingTimeSettingsEmbeddable.setFederalState(null);
        workingTimeSettingsEmbeddable.setWorkingDurationForChristmasEve(null);
        workingTimeSettingsEmbeddable.setWorkingDurationForNewYearsEve(null);
        workingTimeSettingsEmbeddable.setMonday(ZERO);
        workingTimeSettingsEmbeddable.setTuesday(ZERO);
        workingTimeSettingsEmbeddable.setWednesday(ZERO);
        workingTimeSettingsEmbeddable.setThursday(ZERO);
        workingTimeSettingsEmbeddable.setFriday(ZERO);
        workingTimeSettingsEmbeddable.setSaturday(ZERO);
        workingTimeSettingsEmbeddable.setSunday(ZERO);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("workingTimeSettings.federalState", "error.entry.mandatory");
        verify(mockError).rejectValue("workingTimeSettings.workingDurationForChristmasEve", "error.entry.mandatory");
        verify(mockError).rejectValue("workingTimeSettings.workingDurationForNewYearsEve", "error.entry.mandatory");
        verify(mockError).rejectValue("workingTimeSettings.workingDays", "error.entry.mandatory");
    }

    // Working time settings: Overtime settings ------------------------------------------------------------------------
    @Test
    void ensureOvertimeSettingsAreMandatoryIfOvertimeManagementIsActive() {

        final Settings settings = new Settings();
        final OvertimeSettings overtimeSettings = settings.getOvertimeSettings();
        overtimeSettings.setOvertimeActive(true);
        overtimeSettings.setMaximumOvertime(null);
        overtimeSettings.setMinimumOvertime(null);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);

        verify(mockError).rejectValue("overtimeSettings.maximumOvertime", "error.entry.mandatory");
        verify(mockError).rejectValue("overtimeSettings.minimumOvertime", "error.entry.mandatory");
    }

    @Test
    void ensureOvertimeSettingsAreNotMandatoryIfOvertimeManagementIsInactive() {

        final Settings settings = new Settings();
        final OvertimeSettings overtimeSettings = settings.getOvertimeSettings();
        overtimeSettings.setOvertimeActive(false);
        overtimeSettings.setMaximumOvertime(null);
        overtimeSettings.setMinimumOvertime(null);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);

        verifyNoInteractions(mockError);
    }

    @Test
    void ensureMaximumOvertimeCanNotBeNegative() {

        final Settings settings = new Settings();
        final OvertimeSettings overtimeSettings = settings.getOvertimeSettings();
        overtimeSettings.setOvertimeActive(true);
        overtimeSettings.setMaximumOvertime(-1);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);

        verify(mockError).rejectValue("overtimeSettings.maximumOvertime", "error.entry.invalid");
    }

    @Test
    void ensureMinimumOvertimeCanNotBeNegative() {

        final Settings settings = new Settings();
        final OvertimeSettings overtimeSettings = settings.getOvertimeSettings();
        overtimeSettings.setOvertimeActive(true);
        overtimeSettings.setMinimumOvertime(-1);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);

        verify(mockError).rejectValue("overtimeSettings.minimumOvertime", "error.entry.invalid");
    }

    // Absence settings ------------------------------------------------------------------------------------------------
    @Test
    void ensureAbsenceSettingsCanNotBeNull() {

        Settings settings = new Settings();
        ApplicationSettings applicationSettings = settings.getApplicationSettings();

        applicationSettings.setMaximumMonthsToApplyForLeaveInAdvance(null);
        applicationSettings.setDaysBeforeRemindForWaitingApplications(null);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("applicationSettings.maximumMonthsToApplyForLeaveInAdvance", "error.entry.mandatory");
        verify(mockError).rejectValue("applicationSettings.daysBeforeRemindForWaitingApplications", "error.entry.mandatory");
    }

    @Test
    void ensureAbsenceSettingsCanNotBeNegative() {

        Settings settings = new Settings();
        ApplicationSettings applicationSettings = settings.getApplicationSettings();

        applicationSettings.setMaximumMonthsToApplyForLeaveInAdvance(-1);
        applicationSettings.setDaysBeforeRemindForWaitingApplications(-1);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("applicationSettings.maximumMonthsToApplyForLeaveInAdvance", "error.entry.invalid");
        verify(mockError).rejectValue("applicationSettings.daysBeforeRemindForWaitingApplications", "error.entry.invalid");
    }

    @Test
    void ensureThatMaximumMonthsToApplyForLeaveInAdvanceNotZero() {

        Settings settings = new Settings();
        settings.getApplicationSettings().setMaximumMonthsToApplyForLeaveInAdvance(0);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("applicationSettings.maximumMonthsToApplyForLeaveInAdvance", "error.entry.invalid");
    }

    @Test
    void ensureThatAbsenceSettingsAreSmallerOrEqualsThanMaxInt() {

        final Settings settings = new Settings();
        final ApplicationSettings applicationSettings = settings.getApplicationSettings();
        applicationSettings.setMaximumMonthsToApplyForLeaveInAdvance(Integer.MAX_VALUE + 1);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("applicationSettings.maximumMonthsToApplyForLeaveInAdvance", "error.entry.invalid");
    }

    // Account settings ------------------------------------------------------------------------------------------------
    @Test
    void ensureMaximumAnnualVacationDaysAccountSettingsCanNotBeNull() {

        final Settings settings = new Settings();
        final AccountSettings accountSettings = settings.getAccountSettings();
        accountSettings.setMaximumAnnualVacationDays(null);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("accountSettings.maximumAnnualVacationDays", "error.entry.mandatory");
    }

    @Test
    void ensureMaximumAnnualVacationDaysAccountSettingsCanNotBeNegative() {

        final Settings settings = new Settings();
        final AccountSettings accountSettings = settings.getAccountSettings();
        accountSettings.setMaximumAnnualVacationDays(-1);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("accountSettings.maximumAnnualVacationDays", "error.entry.invalid");
    }

    @Test
    void ensureDefaultVacationDaysAccountSettingsCanNotBeNull() {

        final Settings settings = new Settings();
        final AccountSettings accountSettings = settings.getAccountSettings();
        accountSettings.setDefaultVacationDays(null);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("accountSettings.defaultVacationDays", "error.entry.mandatory");
    }

    @Test
    void ensureDefaultVacationDaysAccountSettingsCanNotBeNegative() {

        final Settings settings = new Settings();
        final AccountSettings accountSettings = settings.getAccountSettings();
        accountSettings.setDefaultVacationDays(-1);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("accountSettings.defaultVacationDays", "error.entry.invalid");
    }

    @Test
    void ensureDefaultVacationDaysSmallerThanAYearWithLeapYearForExample() {

        final Settings settings = new Settings();
        settings.getAccountSettings().setDefaultVacationDays(367);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("accountSettings.defaultVacationDays", "error.entry.invalid");
    }

    @Test
    void ensureDefaultVacationDaysSmallerMaxDays() {

        final Settings settings = new Settings();
        settings.getAccountSettings().setMaximumAnnualVacationDays(19);
        settings.getAccountSettings().setDefaultVacationDays(20);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("accountSettings.defaultVacationDays", "settings.account.error.defaultMustBeSmallerOrEqualThanMax");
    }

    // SickNote settings ------------------------------------------------------------------------------------------------
    @Test
    void ensureSickNoteSettingsCanNotBeNull() {

        final Settings settings = new Settings();
        final SickNoteSettings sickNoteSettings = settings.getSickNoteSettings();

        sickNoteSettings.setMaximumSickPayDays(null);
        sickNoteSettings.setDaysBeforeEndOfSickPayNotification(null);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("sickNoteSettings.maximumSickPayDays", "error.entry.mandatory");
        verify(mockError).rejectValue("sickNoteSettings.daysBeforeEndOfSickPayNotification", "error.entry.mandatory");
    }

    @Test
    void ensureSickNoteSettingsCanNotBeSmallerThanLegalMinimum() {

        final Settings settings = new Settings();
        final SickNoteSettings sickNoteSettings = settings.getSickNoteSettings();

        sickNoteSettings.setMaximumSickPayDays(41);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("sickNoteSettings.maximumSickPayDays", "sicknote.error.illegalMaximumSickPayDays");
    }

    @Test
    void ensureDaysBeforeEndOfSickPayNotificationCanNotBeNegative() {

        final Settings settings = new Settings();
        final SickNoteSettings sickNoteSettings = settings.getSickNoteSettings();

        sickNoteSettings.setDaysBeforeEndOfSickPayNotification(-1);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("sickNoteSettings.daysBeforeEndOfSickPayNotification", "error.entry.invalid");
    }

    @Test
    void ensureThatDaysBeforeEndOfSickPayNotificationIsSmallerThanMaximumSickPayDays() {

        final Settings settings = new Settings();
        final SickNoteSettings sickNoteSettings = settings.getSickNoteSettings();
        sickNoteSettings.setDaysBeforeEndOfSickPayNotification(11);
        sickNoteSettings.setMaximumSickPayDays(10);

        final Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("sickNoteSettings.daysBeforeEndOfSickPayNotification", "settings.sickDays.daysBeforeEndOfSickPayNotification.error");
    }

    // Time settings -----------------------------------------------------------------------------------------------
    @Test
    void ensureCalendarSettingsAreMandatory() {

        Settings settings = new Settings();
        TimeSettings timeSettings = settings.getTimeSettings();

        timeSettings.setWorkDayBeginHour(null);
        timeSettings.setWorkDayEndHour(null);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("timeSettings.workDayBeginHour", "error.entry.mandatory");
        verify(mockError).rejectValue("timeSettings.workDayEndHour", "error.entry.mandatory");
    }

    private static Stream<Arguments> invalidWorkingHours() {
        return Stream.of(
            Arguments.of(8, 8), // same hours
            Arguments.of(17, 8), // begin is later than end
            Arguments.of(-1, -2), // negative values
            Arguments.of(25, 42) // more than 24 hours
        );
    }

    @ParameterizedTest
    @MethodSource("invalidWorkingHours")
    void ensureCalendarSettingsAreInvalid(int beginHour, int endHour) {

        Settings settings = new Settings();
        TimeSettings timeSettings = settings.getTimeSettings();

        timeSettings.setWorkDayBeginHour(beginHour);
        timeSettings.setWorkDayEndHour(endHour);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);
        verify(mockError).rejectValue("timeSettings.workDayBeginHour", "error.entry.invalid");
        verify(mockError).rejectValue("timeSettings.workDayEndHour", "error.entry.invalid");
    }

    @Test
    void ensureCalendarSettingsAreValidIfValidWorkDayBeginAndEndHours() {

        Settings settings = new Settings();
        TimeSettings timeSettings = settings.getTimeSettings();

        timeSettings.setWorkDayBeginHour(10);
        timeSettings.setWorkDayEndHour(18);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);

        verifyNoInteractions(mockError);
    }

    // Exchange calendar settings --------------------------------------------------------------------------------------
    @Test
    void ensureExchangeCalendarSettingsAreNotMandatoryIfDeactivated() {

        Settings settings = new Settings();
        ExchangeCalendarSettings exchangeCalendarSettings = settings.getCalendarSettings()
            .getExchangeCalendarSettings();

        exchangeCalendarSettings.setEmail(null);
        exchangeCalendarSettings.setPassword(null);
        exchangeCalendarSettings.setCalendar(null);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);

        verifyNoInteractions(mockError);
    }

    @Test
    void ensureExchangeCalendarSettingsAreMandatory() {

        Settings settings = new Settings();
        ExchangeCalendarSettings exchangeCalendarSettings = settings.getCalendarSettings()
            .getExchangeCalendarSettings();

        settings.getCalendarSettings().setProvider(ExchangeCalendarProvider.class.getSimpleName());
        exchangeCalendarSettings.setEmail(null);
        exchangeCalendarSettings.setPassword(null);
        exchangeCalendarSettings.setCalendar(null);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);

        verify(mockError)
            .rejectValue("calendarSettings.exchangeCalendarSettings.email", "error.entry.mandatory");
        verify(mockError)
            .rejectValue("calendarSettings.exchangeCalendarSettings.password", "error.entry.mandatory");
        verify(mockError)
            .rejectValue("calendarSettings.exchangeCalendarSettings.calendar", "error.entry.mandatory");
    }

    @Test
    void ensureExchangeCalendarEmailMustHaveValidFormat() {

        Settings settings = new Settings();
        ExchangeCalendarSettings exchangeCalendarSettings = settings.getCalendarSettings()
            .getExchangeCalendarSettings();

        settings.getCalendarSettings().setProvider(ExchangeCalendarProvider.class.getSimpleName());
        exchangeCalendarSettings.setEmail("synyx");
        exchangeCalendarSettings.setPassword("top-secret");
        exchangeCalendarSettings.setCalendar("Urlaub");

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);

        verify(mockError).rejectValue("calendarSettings.exchangeCalendarSettings.email", "error.entry.mail");
    }

    @Test
    void ensureGoogleCalendarSettingsAreMandatory() {

        Settings settings = new Settings();
        GoogleCalendarSettings googleCalendarSettings = settings.getCalendarSettings()
            .getGoogleCalendarSettings();

        settings.getCalendarSettings().setProvider(GoogleCalendarSyncProvider.class.getSimpleName());
        googleCalendarSettings.setCalendarId(null);
        googleCalendarSettings.setClientId(null);
        googleCalendarSettings.setClientSecret(null);

        Errors mockError = mock(Errors.class);
        settingsValidator.validate(settings, mockError);

        verify(mockError)
            .rejectValue("calendarSettings.googleCalendarSettings.calendarId", "error.entry.mandatory");
        verify(mockError)
            .rejectValue("calendarSettings.googleCalendarSettings.clientId", "error.entry.mandatory");
        verify(mockError)
            .rejectValue("calendarSettings.googleCalendarSettings.clientSecret", "error.entry.mandatory");
    }
}
