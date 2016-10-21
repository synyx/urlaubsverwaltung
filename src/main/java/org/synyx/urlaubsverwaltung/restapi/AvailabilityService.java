package org.synyx.urlaubsverwaltung.restapi;

import org.joda.time.DateMidnight;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.synyx.urlaubsverwaltung.core.application.domain.Application;
import org.synyx.urlaubsverwaltung.core.application.domain.ApplicationStatus;
import org.synyx.urlaubsverwaltung.core.application.service.ApplicationService;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.settings.FederalState;
import org.synyx.urlaubsverwaltung.core.sicknote.SickNote;
import org.synyx.urlaubsverwaltung.core.sicknote.SickNoteService;
import org.synyx.urlaubsverwaltung.core.workingtime.PublicHolidaysService;
import org.synyx.urlaubsverwaltung.core.workingtime.WorkingTimeService;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author  Marc Kannegiesser - kannegiesser@synyx.de
 */
@Service
public class AvailabilityService {

    private final ApplicationService applicationService;
    private final SickNoteService sickNoteService;
    private final PublicHolidaysService publicHolidaysService;
    private final WorkingTimeService workingTimeService;

    @Autowired
    public AvailabilityService(ApplicationService applicationService, SickNoteService sickNoteService,
        PublicHolidaysService publicHolidaysService, WorkingTimeService workingTimeService) {

        this.applicationService = applicationService;
        this.sickNoteService = sickNoteService;
        this.publicHolidaysService = publicHolidaysService;
        this.workingTimeService = workingTimeService;
    }

    /**
     * Fetch an {@link AvailabilityList} for the given person on all days in the given period of time.
     */
    public AvailabilityList getPersonsAvailabilities(Integer personId, DateMidnight startDate, DateMidnight endDate,
        Person person) {

        Map<DateMidnight, DayAbsence> vacations = getVacations(startDate, endDate, person);
        Map<DateMidnight, DayAbsence> sickNotes = getSickNotes(startDate, endDate, person);

        List<DayAvailability> availabilities = new ArrayList<>();

        DateMidnight currentDay = startDate;

        while (!currentDay.isAfter(endDate)) {
            availabilities.add(getAvailabilityfor(currentDay, person, vacations, sickNotes));

            currentDay = currentDay.plusDays(1);
        }

        return new AvailabilityList(availabilities, personId);
    }


    private DayAvailability getAvailabilityfor(DateMidnight currentDay, Person person,
        Map<DateMidnight, DayAbsence> vacations, Map<DateMidnight, DayAbsence> sickNotes) {

        // todo this should be configurable!
        BigDecimal regularWorkHours = new BigDecimal(8);

        DayAvailability.Absence.Type freeType = DayAvailability.Absence.Type.FREETIME;

        BigDecimal expectedHoursToWork = publicHolidaysService.getWorkingDurationOfDate(currentDay,
                getFederalState(currentDay, person));

        if (BigDecimal.ZERO.equals(expectedHoursToWork)) {
            freeType = DayAvailability.Absence.Type.HOLIDAY;
        }

        DayAbsence vacation = vacations.get(currentDay);
        DayAbsence sick = sickNotes.get(currentDay);

        List<DayAvailability.Absence> spans = new ArrayList<>();

        BigDecimal hoursOfAbsence = BigDecimal.ZERO;

        if (vacation != null) {
            BigDecimal hours = vacation.getDayLength().multiply(regularWorkHours);
            DayAvailability.Absence span = new DayAvailability.TimedAbsence(hours,
                    DayAvailability.Absence.Type.VACATION);
            spans.add(span);
            hoursOfAbsence = hoursOfAbsence.add(hours);
        }

        if (sick != null) {
            BigDecimal hours = sick.getDayLength().multiply(regularWorkHours);
            DayAvailability.Absence span = new DayAvailability.TimedAbsence(hours,
                    DayAvailability.Absence.Type.SICK_NOTE);
            spans.add(span);
            hoursOfAbsence = hoursOfAbsence.add(hours);
        }

        BigDecimal presenceHours;

        if (expectedHoursToWork.compareTo(hoursOfAbsence) > 0) {
            presenceHours = expectedHoursToWork.subtract(hoursOfAbsence);
        } else {
            presenceHours = BigDecimal.ZERO;
        }

        // if there was no work expected it is either freetime or an holiday
        // FIXME this has to be done only when presenceHours == 0
        if (spans.isEmpty()) {
            DayAvailability.Absence span = new DayAvailability.Absence(freeType);
            spans.add(span);
        }

        return new DayAvailability(presenceHours, currentDay.toString("yyyy-MM-dd"), spans);
    }


    private FederalState getFederalState(DateMidnight date, Person person) {

        return workingTimeService.getFederalStateForPerson(person, date);
    }


    private Map<DateMidnight, DayAbsence> getVacations(DateMidnight start, DateMidnight end, Person person) {

        Map<DateMidnight, DayAbsence> absenceMap = new HashMap<>();

        List<Application> applications = applicationService.getApplicationsForACertainPeriodAndPerson(start, end,
                    person)
                .stream()
                .filter(application ->
                            application.hasStatus(ApplicationStatus.WAITING)
                            || application.hasStatus(ApplicationStatus.TEMPORARY_ALLOWED)
                            || application.hasStatus(ApplicationStatus.ALLOWED))
                .collect(Collectors.toList());

        for (Application application : applications) {
            DateMidnight startDate = application.getStartDate();
            DateMidnight endDate = application.getEndDate();

            DateMidnight day = startDate;

            while (!day.isAfter(endDate)) {
                if (!day.isBefore(start) && !day.isAfter(end)) {
                    absenceMap.put(day,
                        new DayAbsence(day, application.getDayLength(), DayAbsence.Type.VACATION,
                            application.getStatus().name(), application.getId()));
                }

                day = day.plusDays(1);
            }
        }

        return absenceMap;
    }


    private Map<DateMidnight, DayAbsence> getSickNotes(DateMidnight start, DateMidnight end, Person person) {

        Map<DateMidnight, DayAbsence> absenceMap = new HashMap<>();

        List<SickNote> sickNotes = sickNoteService.getByPersonAndPeriod(person, start, end)
                .stream()
                .filter(SickNote::isActive)
                .collect(Collectors.toList());

        for (SickNote sickNote : sickNotes) {
            DateMidnight startDate = sickNote.getStartDate();
            DateMidnight endDate = sickNote.getEndDate();

            DateMidnight day = startDate;

            while (!day.isAfter(endDate)) {
                if (!day.isBefore(start) && !day.isAfter(end)) {
                    absenceMap.put(day,
                        new DayAbsence(day, sickNote.getDayLength(), DayAbsence.Type.SICK_NOTE, "ACTIVE",
                            sickNote.getId()));
                }

                day = day.plusDays(1);
            }
        }

        return absenceMap;
    }
}
