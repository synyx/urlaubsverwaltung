package org.synyx.urlaubsverwaltung.web.application;

import org.joda.time.DateMidnight;

import org.junit.Before;
import org.junit.Test;

import org.mockito.Mockito;

import org.springframework.validation.Errors;

import org.synyx.urlaubsverwaltung.core.application.domain.Application;
import org.synyx.urlaubsverwaltung.core.application.domain.VacationType;
import org.synyx.urlaubsverwaltung.core.application.service.CalculationService;
import org.synyx.urlaubsverwaltung.core.calendar.OverlapCase;
import org.synyx.urlaubsverwaltung.core.calendar.OverlapService;
import org.synyx.urlaubsverwaltung.core.calendar.WorkDaysService;
import org.synyx.urlaubsverwaltung.core.calendar.workingtime.WorkingTimeService;
import org.synyx.urlaubsverwaltung.core.period.DayLength;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.settings.Settings;
import org.synyx.urlaubsverwaltung.core.settings.SettingsService;
import org.synyx.urlaubsverwaltung.test.TestDataCreator;

import java.math.BigDecimal;

import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


/**
 * Unit test for {@link ApplicationValidator}.
 *
 * @author  Aljona Murygina - murygina@synyx.de
 */
public class ApplicationValidatorTest {

    private ApplicationValidator validator;

    private WorkingTimeService workingTimeService;
    private WorkDaysService calendarService;
    private OverlapService overlapService;
    private CalculationService calculationService;
    private SettingsService settingsService;

    private Errors errors;
    private ApplicationForLeaveForm appForm;

    private Settings settings;

    @Before
    public void setUp() {

        settingsService = Mockito.mock(SettingsService.class);
        settings = new Settings();
        Mockito.when(settingsService.getSettings()).thenReturn(settings);

        calendarService = Mockito.mock(WorkDaysService.class);
        overlapService = Mockito.mock(OverlapService.class);
        calculationService = Mockito.mock(CalculationService.class);
        workingTimeService = Mockito.mock(WorkingTimeService.class);

        validator = new ApplicationValidator(workingTimeService, calendarService, overlapService, calculationService,
                settingsService);
        errors = Mockito.mock(Errors.class);

        appForm = new ApplicationForLeaveForm();

        appForm.setVacationType(VacationType.HOLIDAY);
        appForm.setDayLength(DayLength.FULL);
        appForm.setStartDate(DateMidnight.now());
        appForm.setEndDate(DateMidnight.now().plusDays(2));

        // Default: everything is alright, override for negative cases
        Mockito.when(errors.hasErrors()).thenReturn(Boolean.FALSE);

        Mockito.when(workingTimeService.getByPersonAndValidityDateEqualsOrMinorDate(Mockito.any(Person.class),
                    Mockito.any(DateMidnight.class)))
            .thenReturn(Optional.of(TestDataCreator.createWorkingTime()));
        Mockito.when(calendarService.getWorkDays(Mockito.any(DayLength.class), Mockito.any(DateMidnight.class),
                    Mockito.any(DateMidnight.class), Mockito.any(Person.class)))
            .thenReturn(BigDecimal.ONE);
        Mockito.when(overlapService.checkOverlap(Mockito.any(Application.class)))
            .thenReturn(OverlapCase.NO_OVERLAPPING);
        Mockito.when(calculationService.checkApplication(Mockito.any(Application.class))).thenReturn(Boolean.TRUE);
    }


    // Supports --------------------------------------------------------------------------------------------------------

    @Test
    public void ensureSupportsAppFormClass() {

        assertTrue(validator.supports(ApplicationForLeaveForm.class));
    }


    @Test
    public void ensureDoesNotSupportNull() {

        assertFalse(validator.supports(null));
    }


    @Test
    public void ensureDoesNotSupportOtherClass() {

        assertFalse(validator.supports(Person.class));
    }


    // Validate period -------------------------------------------------------------------------------------------------

    @Test
    public void ensureStartDateIsMandatory() {

        appForm.setDayLength(DayLength.FULL);
        appForm.setStartDate(null);

        validator.validate(appForm, errors);

        Mockito.verify(errors).rejectValue("startDate", "error.entry.mandatory");
    }


    @Test
    public void ensureEndDateIsMandatory() {

        appForm.setDayLength(DayLength.FULL);
        appForm.setEndDate(null);

        validator.validate(appForm, errors);

        Mockito.verify(errors).rejectValue("endDate", "error.entry.mandatory");
    }


    @Test
    public void ensureStartDateIsNotMandatoryForHalfDays() {

        appForm.setDayLength(DayLength.MORNING);
        appForm.setStartDateHalf(DateMidnight.now());

        appForm.setStartDate(null);

        validator.validate(appForm, errors);

        Mockito.verify(errors, Mockito.never()).reject(Mockito.anyString());
        Mockito.verify(errors, Mockito.never()).rejectValue(Mockito.anyString(), Mockito.anyString());
    }


    @Test
    public void ensureStartDateHalfIsNotMandatoryForFullDays() {

        appForm.setDayLength(DayLength.FULL);
        appForm.setStartDateHalf(null);

        validator.validate(appForm, errors);

        Mockito.verify(errors, Mockito.never()).reject(Mockito.anyString());
        Mockito.verify(errors, Mockito.never()).rejectValue(Mockito.anyString(), Mockito.anyString());
    }


    @Test
    public void ensureStartDateMustBeBeforeEndDate() {

        appForm.setDayLength(DayLength.FULL);
        appForm.setStartDate(new DateMidnight(2012, 1, 17));
        appForm.setEndDate(new DateMidnight(2012, 1, 12));

        validator.validate(appForm, errors);

        Mockito.verify(errors).reject("error.entry.invalidPeriod");
    }


    @Test
    public void ensureVeryPastDateIsNotValid() {

        DateMidnight pastDate = DateMidnight.now().minusYears(10);

        appForm.setDayLength(DayLength.FULL);
        appForm.setStartDate(pastDate);
        appForm.setEndDate(pastDate.plusDays(1));

        validator.validate(appForm, errors);

        Mockito.verify(errors).reject("application.error.tooFarInThePast");
    }


    @Test
    public void ensureVeryFutureDateIsNotValid() {

        DateMidnight futureDate = DateMidnight.now().plusYears(10);

        appForm.setDayLength(DayLength.FULL);
        appForm.setStartDate(futureDate);
        appForm.setEndDate(futureDate.plusDays(1));

        validator.validate(appForm, errors);

        Mockito.verify(errors)
            .reject("application.error.tooFarInTheFuture",
                new Object[] { settings.getAbsenceSettings().getMaximumMonthsToApplyForLeaveInAdvance().toString() },
                null);
    }


    @Test
    public void ensureVeryPastDateForHalfDayIsNotValid() {

        DateMidnight pastDate = DateMidnight.now().minusYears(10);

        appForm.setDayLength(DayLength.MORNING);
        appForm.setStartDateHalf(pastDate);

        validator.validate(appForm, errors);

        Mockito.verify(errors).reject("application.error.tooFarInThePast");
    }


    @Test
    public void ensureVeryFutureDateForHalfDayIsNotValid() {

        DateMidnight futureDate = DateMidnight.now().plusYears(10);

        appForm.setDayLength(DayLength.NOON);
        appForm.setStartDateHalf(futureDate);

        validator.validate(appForm, errors);

        Mockito.verify(errors)
            .reject("application.error.tooFarInTheFuture",
                new Object[] { settings.getAbsenceSettings().getMaximumMonthsToApplyForLeaveInAdvance().toString() },
                null);
    }


    // Validate reason -------------------------------------------------------------------------------------------------

    @Test
    public void ensureReasonIsNotMandatoryForHoliday() {

        appForm.setVacationType(VacationType.HOLIDAY);
        appForm.setReason("");

        validator.validate(appForm, errors);

        Mockito.verify(errors, Mockito.never()).reject(Mockito.anyString());
        Mockito.verify(errors, Mockito.never()).rejectValue(Mockito.eq("reason"), Mockito.anyString());
    }


    @Test
    public void ensureReasonIsNotMandatoryForUnpaidLeave() {

        appForm.setVacationType(VacationType.UNPAIDLEAVE);
        appForm.setReason("");

        validator.validate(appForm, errors);

        Mockito.verify(errors, Mockito.never()).reject(Mockito.anyString());
        Mockito.verify(errors, Mockito.never()).rejectValue(Mockito.eq("reason"), Mockito.anyString());
    }


    @Test
    public void ensureReasonIsNotMandatoryForOvertime() {

        appForm.setVacationType(VacationType.OVERTIME);
        appForm.setReason("");

        validator.validate(appForm, errors);

        Mockito.verify(errors, Mockito.never()).reject(Mockito.anyString());
        Mockito.verify(errors, Mockito.never()).rejectValue(Mockito.eq("reason"), Mockito.anyString());
    }


    @Test
    public void ensureReasonIsMandatoryForSpecialLeave() {

        appForm.setVacationType(VacationType.SPECIALLEAVE);
        appForm.setReason("");

        validator.validate(appForm, errors);

        Mockito.verify(errors).rejectValue("reason", "application.error.missingReasonForSpecialLeave");
    }


    // Validate address ------------------------------------------------------------------------------------------------

    @Test
    public void ensureThereIsAMaximumCharLength() {

        appForm.setAddress(
            "Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod tincidunt"
            + " ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, quis nostrud "
            + "exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. ");

        validator.validate(appForm, errors);

        Mockito.verify(errors).rejectValue("address", "error.entry.tooManyChars");
    }


    // Validate vacation days ------------------------------------------------------------------------------------------

    @Test
    public void ensureApplicationForLeaveWithZeroVacationDaysIsNotValid() {

        Mockito.when(errors.hasErrors()).thenReturn(Boolean.FALSE);

        Mockito.when(calendarService.getWorkDays(Mockito.any(DayLength.class), Mockito.any(DateMidnight.class),
                    Mockito.any(DateMidnight.class), Mockito.any(Person.class)))
            .thenReturn(BigDecimal.ZERO);

        validator.validate(appForm, errors);

        Mockito.verify(errors).reject("application.error.zeroDays");

        Mockito.verifyZeroInteractions(overlapService);
        Mockito.verifyZeroInteractions(calculationService);
    }


    @Test
    public void ensureApplyingForLeaveWithNotEnoughVacationDaysIsNotValid() {

        Mockito.when(errors.hasErrors()).thenReturn(Boolean.FALSE);

        Mockito.when(calendarService.getWorkDays(Mockito.any(DayLength.class), Mockito.any(DateMidnight.class),
                    Mockito.any(DateMidnight.class), Mockito.any(Person.class)))
            .thenReturn(BigDecimal.ONE);

        Mockito.when(overlapService.checkOverlap(Mockito.any(Application.class)))
            .thenReturn(OverlapCase.NO_OVERLAPPING);

        Mockito.when(calculationService.checkApplication(Mockito.any(Application.class))).thenReturn(Boolean.FALSE);

        validator.validate(appForm, errors);

        Mockito.verify(errors).reject("application.error.notEnoughVacationDays");
    }


    @Test
    public void ensureApplyingHalfDayForLeaveWithNotEnoughVacationDaysIsNotValid() {

        appForm.setDayLength(DayLength.NOON);
        appForm.setStartDateHalf(DateMidnight.now());

        Mockito.when(errors.hasErrors()).thenReturn(Boolean.FALSE);

        Mockito.when(calendarService.getWorkDays(Mockito.eq(appForm.getDayLength()),
                    Mockito.eq(appForm.getStartDateHalf()), Mockito.eq(appForm.getStartDateHalf()),
                    Mockito.eq(appForm.getPerson())))
            .thenReturn(BigDecimal.ONE);

        Mockito.when(overlapService.checkOverlap(Mockito.any(Application.class)))
            .thenReturn(OverlapCase.NO_OVERLAPPING);

        Mockito.when(calculationService.checkApplication(Mockito.any(Application.class))).thenReturn(Boolean.FALSE);

        validator.validate(appForm, errors);

        Mockito.verify(errors).reject("application.error.notEnoughVacationDays");
    }


    // Validate overlapping --------------------------------------------------------------------------------------------

    @Test
    public void ensureOverlappingApplicationForLeaveIsNotValid() {

        Mockito.when(errors.hasErrors()).thenReturn(Boolean.FALSE);

        Mockito.when(calendarService.getWorkDays(Mockito.any(DayLength.class), Mockito.any(DateMidnight.class),
                    Mockito.any(DateMidnight.class), Mockito.any(Person.class)))
            .thenReturn(BigDecimal.ONE);

        Mockito.when(overlapService.checkOverlap(Mockito.any(Application.class)))
            .thenReturn(OverlapCase.FULLY_OVERLAPPING);

        validator.validate(appForm, errors);

        Mockito.verify(errors).reject("application.error.overlap");

        Mockito.verifyZeroInteractions(calculationService);
    }


    // Validate hours --------------------------------------------------------------------------------------------------

    @Test
    public void ensureHoursIsMandatoryForOvertime() {

        appForm.setVacationType(VacationType.OVERTIME);
        appForm.setHours(null);

        validator.validate(appForm, errors);

        Mockito.verify(errors).rejectValue("hours", "application.error.missingHoursForOvertime");
    }


    @Test
    public void ensureHoursIsNotMandatoryForOtherTypesOfVacation() {

        Consumer<VacationType> assertHoursNotMandatory = (type) -> {
            appForm.setVacationType(type);
            appForm.setHours(null);

            validator.validate(appForm, errors);

            Mockito.verify(errors, Mockito.never()).rejectValue(Mockito.eq("hours"), Mockito.anyString());
        };

        assertHoursNotMandatory.accept(VacationType.HOLIDAY);
        assertHoursNotMandatory.accept(VacationType.SPECIALLEAVE);
        assertHoursNotMandatory.accept(VacationType.UNPAIDLEAVE);
    }


    @Test
    public void ensureHoursCanNotBeZero() {

        appForm.setHours(BigDecimal.ZERO);

        validator.validate(appForm, errors);

        Mockito.verify(errors).rejectValue("hours", "application.error.invalidHoursForOvertime");
    }


    @Test
    public void ensureHoursCanNotBeNegative() {

        appForm.setHours(BigDecimal.ONE.negate());

        validator.validate(appForm, errors);

        Mockito.verify(errors).rejectValue("hours", "application.error.invalidHoursForOvertime");
    }


    @Test
    public void ensureDecimalHoursAreValid() {

        appForm.setVacationType(VacationType.OVERTIME);
        appForm.setHours(new BigDecimal("0.5"));

        validator.validate(appForm, errors);

        Mockito.verify(errors, Mockito.never()).rejectValue(Mockito.eq("hours"), Mockito.anyString());
    }


    @Test
    public void ensureNoErrorMessageForMandatoryIfHoursIsNullBecauseOfTypeMismatch() {

        appForm.setVacationType(VacationType.OVERTIME);
        appForm.setHours(null);

        Mockito.when(errors.hasFieldErrors("hours")).thenReturn(true);

        validator.validate(appForm, errors);

        Mockito.verify(errors).hasFieldErrors("hours");
        Mockito.verify(errors, Mockito.never()).rejectValue("hours", "application.error.missingHoursForOvertime");
    }


    // Validate working time exists ------------------------------------------------------------------------------------

    @Test
    public void ensureWorkingTimeConfigurationMustExistForPeriodOfSickNote() {

        Mockito.when(errors.hasErrors()).thenReturn(Boolean.FALSE);

        Mockito.when(workingTimeService.getByPersonAndValidityDateEqualsOrMinorDate(Mockito.any(Person.class),
                    Mockito.any(DateMidnight.class)))
            .thenReturn(Optional.empty());

        validator.validate(appForm, errors);

        Mockito.verify(errors).reject("application.error.noValidWorkingTime");

        Mockito.verifyZeroInteractions(calendarService);
        Mockito.verifyZeroInteractions(overlapService);
        Mockito.verifyZeroInteractions(calculationService);
    }
}
