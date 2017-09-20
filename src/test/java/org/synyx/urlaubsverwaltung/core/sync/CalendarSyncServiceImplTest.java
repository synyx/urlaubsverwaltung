package org.synyx.urlaubsverwaltung.core.sync;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import org.synyx.urlaubsverwaltung.core.settings.CalendarSettings;
import org.synyx.urlaubsverwaltung.core.settings.ExchangeCalendarSettings;
import org.synyx.urlaubsverwaltung.core.settings.Settings;
import org.synyx.urlaubsverwaltung.core.settings.SettingsService;
import org.synyx.urlaubsverwaltung.core.sync.absence.Absence;

import java.util.Optional;


/**
 * Unit test for {@link CalendarSyncServiceImpl}.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
public class CalendarSyncServiceImplTest {

    private SettingsService settingsService;
    private CalendarProviderService exchangeCalendarProviderService;

    private CalendarSyncService calendarSyncService;

    private Settings settings;

    @Before
    public void setUp() {

        settingsService = Mockito.mock(SettingsService.class);
        exchangeCalendarProviderService = Mockito.mock(ExchangeCalendarProviderService.class);

        calendarSyncService = new CalendarSyncServiceImpl(settingsService, exchangeCalendarProviderService);

        settings = new Settings();
        Mockito.when(settingsService.getSettings()).thenReturn(settings);
    }


    @Test
    public void ensureAddsAbsenceToExchangeCalendarIfActivated() {

        ExchangeCalendarSettings calendarSettings = settings.getCalendarSettings().getExchangeCalendarSettings();
        calendarSettings.setActive(true);

        Absence absence = Mockito.mock(Absence.class);

        calendarSyncService.addAbsence(absence);

        Mockito.verify(exchangeCalendarProviderService)
            .addAbsence(Mockito.eq(absence), Mockito.eq(settings.getCalendarSettings()));
    }


    @Test
    public void ensureDoesNotAddAbsenceToExchangeCalendarIfDeactivated() {

        ExchangeCalendarSettings calendarSettings = settings.getCalendarSettings().getExchangeCalendarSettings();
        calendarSettings.setActive(false);

        Absence absence = Mockito.mock(Absence.class);

        Optional<String> optionalEventId = calendarSyncService.addAbsence(absence);

        Mockito.verifyZeroInteractions(exchangeCalendarProviderService);

        Assert.assertFalse("Should be empty", optionalEventId.isPresent());
    }


    @Test
    public void ensureUpdatesAbsenceInExchangeCalendarIfActivated() {

        ExchangeCalendarSettings calendarSettings = settings.getCalendarSettings().getExchangeCalendarSettings();
        calendarSettings.setActive(true);

        Absence absence = Mockito.mock(Absence.class);
        String eventId = "event-1";

        calendarSyncService.update(absence, eventId);

        Mockito.verify(exchangeCalendarProviderService)
            .update(Mockito.eq(absence), Mockito.eq(eventId), Mockito.eq(settings.getCalendarSettings()));
    }


    @Test
    public void ensureDoesNotUpdateAbsenceInExchangeCalendarIfDeactivated() {

        ExchangeCalendarSettings calendarSettings = settings.getCalendarSettings().getExchangeCalendarSettings();
        calendarSettings.setActive(false);

        Absence absence = Mockito.mock(Absence.class);
        String eventId = "event-1";

        calendarSyncService.update(absence, eventId);

        Mockito.verifyZeroInteractions(exchangeCalendarProviderService);
    }


    @Test
    public void ensureDeletedAbsenceInExchangeCalendarIfActivated() {

        ExchangeCalendarSettings calendarSettings = settings.getCalendarSettings().getExchangeCalendarSettings();
        calendarSettings.setActive(true);

        String eventId = "event-1";

        calendarSyncService.deleteAbsence(eventId);

        Mockito.verify(exchangeCalendarProviderService)
            .deleteAbsence(Mockito.eq(eventId), Mockito.eq(settings.getCalendarSettings()));
    }


    @Test
    public void ensureDoesNotDeleteAbsenceInExchangeCalendarIfDeactivated() {

        ExchangeCalendarSettings calendarSettings = settings.getCalendarSettings().getExchangeCalendarSettings();
        calendarSettings.setActive(false);

        String eventId = "event-1";

        calendarSyncService.deleteAbsence(eventId);

        Mockito.verifyZeroInteractions(exchangeCalendarProviderService);
    }


    @Test
    public void ensureChecksExchangeCalendarSettingsIfActivated() {

        ExchangeCalendarSettings calendarSettings = settings.getCalendarSettings().getExchangeCalendarSettings();
        calendarSettings.setActive(true);

        calendarSyncService.checkCalendarSyncSettings();

        Mockito.verify(exchangeCalendarProviderService).checkCalendarSyncSettings(Mockito.any(CalendarSettings.class));
    }


    @Test
    public void ensureDoesNotCheckExchangeCalendarSettingsIfNotActivated() {

        ExchangeCalendarSettings calendarSettings = settings.getCalendarSettings().getExchangeCalendarSettings();
        calendarSettings.setActive(false);

        calendarSyncService.checkCalendarSyncSettings();

        Mockito.verifyZeroInteractions(exchangeCalendarProviderService);
    }
}
