package org.synyx.urlaubsverwaltung.calendar;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.synyx.urlaubsverwaltung.absence.Absence;
import org.synyx.urlaubsverwaltung.absence.AbsenceService;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.person.Role;

import java.io.File;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
class CompanyCalendarService {

    private final AbsenceService absenceService;
    private final DepartmentService departmentService;
    private final CompanyCalendarRepository companyCalendarRepository;
    private final ICalService iCalService;
    private final PersonService personService;
    private final MessageSource messageSource;
    private final Clock clock;

    @Autowired
    CompanyCalendarService(AbsenceService absenceService, DepartmentService departmentService, CompanyCalendarRepository companyCalendarRepository, ICalService iCalService, PersonService personService, MessageSource messageSource, Clock clock) {
        this.absenceService = absenceService;
        this.departmentService = departmentService;
        this.companyCalendarRepository = companyCalendarRepository;
        this.iCalService = iCalService;
        this.personService = personService;
        this.messageSource = messageSource;
        this.clock = clock;
    }

    CompanyCalendar createCalendarForPerson(int personId, Period calendarPeriod) {

        final Person person = getPersonOrThrow(personId);

        final Optional<CompanyCalendar> maybeCompanyCalendar = companyCalendarRepository.findByPerson(person);
        final CompanyCalendar companyCalendar = maybeCompanyCalendar.isEmpty() ? new CompanyCalendar() : maybeCompanyCalendar.get();
        companyCalendar.setPerson(person);
        companyCalendar.setCalendarPeriod(calendarPeriod);
        companyCalendar.generateSecret();

        return companyCalendarRepository.save(companyCalendar);
    }

    Optional<CompanyCalendar> getCompanyCalendar(int personId) {

        final Person person = getPersonOrThrow(personId);

        return companyCalendarRepository.findByPerson(person);
    }

    File getCalendarForAll(Integer personId, String secret, Locale locale) {

        if (StringUtils.isBlank(secret)) {
            throw new IllegalArgumentException("secret must not be empty.");
        }

        final Person person = getPersonOrThrow(personId);
        final Optional<CompanyCalendar> maybeCompanyCalendar = companyCalendarRepository.findBySecretAndPerson(secret, person);
        if (maybeCompanyCalendar.isEmpty()) {
            throw new IllegalArgumentException("No calendar found for secret=" + secret);
        }

        final String title = messageSource.getMessage("calendar.company.title", new Object[]{}, locale);

        final CompanyCalendar companyCalendar = maybeCompanyCalendar.get();
        final LocalDate sinceDate = LocalDate.now(clock).minus(companyCalendar.getCalendarPeriod());

        // for OFFICE, show everyone
        // for others, show everyone from visible departments
        final List<Absence> absences;
        if (person.hasRole(Role.OFFICE)) {
            absences = absenceService.getOpenAbsencesSince(sinceDate);
        } else {
            var departments = departmentService.getAllowedDepartmentsOfPerson(person);
            var persons = departments.stream().flatMap(d -> d.getMembers().stream()).distinct().collect(Collectors.toList());
            absences = absenceService.getOpenAbsencesSince(persons, sinceDate);
        }

        return iCalService.getCalendar(title, absences);
    }

    @Transactional
    public void deleteCalendarForPerson(int personId) {

        final Person person = getPersonOrThrow(personId);

        companyCalendarRepository.deleteByPerson(person);
    }

    /**
     * Delete all {@link CompanyCalendar} for persons who don't have one of the given {@link Role}.
     *
     * @param roles
     */
    @Transactional
    public void deleteCalendarsForPersonsWithoutOneOfRole(Role... roles) {

        final List<Role> roleList = Arrays.asList(roles);

        for (final Person person : personService.getActivePersons()) {
            if (roleList.stream().noneMatch(person::hasRole)) {
                companyCalendarRepository.deleteByPerson(person);
            }
        }
    }

    private Person getPersonOrThrow(Integer personId) {

        final Optional<Person> maybePerson = personService.getPersonByID(personId);
        if (maybePerson.isEmpty()) {
            throw new IllegalArgumentException("could not find person for given personId=" + personId);
        }

        return maybePerson.get();
    }
}
