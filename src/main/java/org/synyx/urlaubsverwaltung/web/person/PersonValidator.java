
package org.synyx.urlaubsverwaltung.web.person;

import org.joda.time.DateMidnight;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import org.springframework.util.StringUtils;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import org.synyx.urlaubsverwaltung.core.person.MailNotification;
import org.synyx.urlaubsverwaltung.core.person.Person;
import org.synyx.urlaubsverwaltung.core.person.PersonService;
import org.synyx.urlaubsverwaltung.core.person.Role;
import org.synyx.urlaubsverwaltung.core.settings.AbsenceSettings;
import org.synyx.urlaubsverwaltung.core.settings.Settings;
import org.synyx.urlaubsverwaltung.core.settings.SettingsService;
import org.synyx.urlaubsverwaltung.web.MailAddressValidationUtil;

import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;


/**
 * This class validate if a {@link PersonForm} is filled correctly by the user, else it saves error messages in errors
 * object.
 *
 * @author  Aljona Murygina
 */
@Component
public class PersonValidator implements Validator {

    private static final String ERROR_MANDATORY_FIELD = "error.entry.mandatory";
    private static final String ERROR_ENTRY = "error.entry.invalid";
    private static final String ERROR_LENGTH = "error.entry.tooManyChars";
    private static final String ERROR_EMAIL = "error.entry.mail";
    private static final String ERROR_PERIOD = "error.entry.invalidPeriod";
    private static final String ERROR_LOGIN_UNIQUE = "person.form.data.login.error";
    private static final String ERROR_PERMISSIONS_MANDATORY = "person.form.permissions.error.mandatory";
    private static final String ERROR_PERMISSIONS_INACTIVE_ROLE = "person.form.permissions.error.inactive";
    private static final String ERROR_PERMISSIONS_USER_ROLE = "person.form.permissions.error.user";
    private static final String ERROR_PERMISSIONS_COMBINATION = "person.form.permissions.error.combination";
    private static final String ERROR_NOTIFICATIONS_COMBINATION = "person.form.notifications.error.combination";
    private static final String ERROR_WORKING_TIME_MANDATORY = "person.form.workingTime.error.mandatory";

    private static final String ATTRIBUTE_LOGIN_NAME = "loginName";
    private static final String ATTRIBUTE_FIRST_NAME = "firstName";
    private static final String ATTRIBUTE_LAST_NAME = "lastName";
    private static final String ATTRIBUTE_ANNUAL_VACATION_DAYS = "annualVacationDays";
    private static final String ATTRIBUTE_ACTUAL_VACATION_DAYS = "actualVacationDays";
    private static final String ATTRIBUTE_REMAINING_VACATION_DAYS = "remainingVacationDays";
    private static final String ATTRIBUTE_REMAINING_VACATION_DAYS_NOT_EXPIRING = "remainingVacationDaysNotExpiring";
    private static final String ATTRIBUTE_EMAIL = "email";
    private static final String ATTRIBUTE_PERMISSIONS = "permissions";

    private static final int MAX_CHARS = 50;

    private final PersonService personService;
    private final SettingsService settingsService;

    @Autowired
    public PersonValidator(PersonService personService, SettingsService settingsService) {

        this.personService = personService;
        this.settingsService = settingsService;
    }

    @Override
    public boolean supports(Class<?> clazz) {

        return PersonForm.class.equals(clazz);
    }


    @Override
    public void validate(Object target, Errors errors) {

        PersonForm form = (PersonForm) target;

        validateName(form.getFirstName(), ATTRIBUTE_FIRST_NAME, errors);

        validateName(form.getLastName(), ATTRIBUTE_LAST_NAME, errors);

        validateEmail(form.getEmail(), errors);

        validatePeriod(form, errors);

        validateValidFrom(form, errors);

        validateAnnualVacation(form, errors);

        validateActualVacation(form, errors);

        validateRemainingVacationDays(form, errors);

        validatePermissions(form, errors);

        validateNotifications(form, errors);

        validateWorkingTimes(form, errors);
    }


    void validateName(String name, String field, Errors errors) {

        if (StringUtils.hasText(name)) {
            if (!validateStringLength(name)) {
                errors.rejectValue(field, ERROR_LENGTH);
            }
        } else {
            errors.rejectValue(field, ERROR_MANDATORY_FIELD);
        }
    }


    public void validateLogin(String login, Errors errors) {

        validateName(login, ATTRIBUTE_LOGIN_NAME, errors);

        if (!errors.hasFieldErrors(ATTRIBUTE_LOGIN_NAME)) {
            // validate unique login name
            Optional<Person> person = personService.getPersonByLogin(login);

            if (person.isPresent()) {
                errors.rejectValue(ATTRIBUTE_LOGIN_NAME, ERROR_LOGIN_UNIQUE);
            }
        }
    }


    void validateEmail(String email, Errors errors) {

        if (StringUtils.hasText(email)) {
            if (!validateStringLength(email)) {
                errors.rejectValue(ATTRIBUTE_EMAIL, ERROR_LENGTH);
            }

            if (!MailAddressValidationUtil.hasValidFormat(email)) {
                errors.rejectValue(ATTRIBUTE_EMAIL, ERROR_EMAIL);
            }
        } else {
            errors.rejectValue(ATTRIBUTE_EMAIL, ERROR_MANDATORY_FIELD);
        }
    }


    void validatePeriod(PersonForm form, Errors errors) {

        DateMidnight holidaysAccountValidFrom = form.getHolidaysAccountValidFrom();
        DateMidnight holidaysAccountValidTo = form.getHolidaysAccountValidTo();

        validateDateNotNull(holidaysAccountValidFrom, "holidaysAccountValidFrom", errors);
        validateDateNotNull(holidaysAccountValidTo, "holidaysAccountValidTo", errors);

        if (holidaysAccountValidFrom != null && holidaysAccountValidTo != null) {
            boolean periodIsNotWithinOneYear = holidaysAccountValidFrom.getYear() != form.getHolidaysAccountYear()
                || holidaysAccountValidTo.getYear() != form.getHolidaysAccountYear();
            boolean periodIsOnlyOneDay = holidaysAccountValidFrom.equals(holidaysAccountValidTo);
            boolean beginOfPeriodIsAfterEndOfPeriod = holidaysAccountValidFrom.isAfter(holidaysAccountValidTo);

            if (periodIsNotWithinOneYear || periodIsOnlyOneDay || beginOfPeriodIsAfterEndOfPeriod) {
                errors.reject(ERROR_PERIOD);
            }
        }
    }


    private void validateDateNotNull(DateMidnight date, String field, Errors errors) {

        // may be that date field is null because of cast exception, than there is already a field error
        if (date == null && errors.getFieldErrors(field).isEmpty()) {
            errors.rejectValue(field, ERROR_MANDATORY_FIELD);
        }
    }


    private void validateValidFrom(PersonForm form, Errors errors) {

        validateDateNotNull(form.getValidFrom(), "validFrom", errors);
    }


    void validateAnnualVacation(PersonForm form, Errors errors) {

        BigDecimal annualVacationDays = form.getAnnualVacationDays();
        Settings settings = settingsService.getSettings();
        AbsenceSettings absenceSettings = settings.getAbsenceSettings();
        BigDecimal maxDays = BigDecimal.valueOf(absenceSettings.getMaximumAnnualVacationDays());

        validateNumberNotNull(annualVacationDays, ATTRIBUTE_ANNUAL_VACATION_DAYS, errors);

        if (annualVacationDays != null) {
            validateNumberOfDays(annualVacationDays, ATTRIBUTE_ANNUAL_VACATION_DAYS, maxDays, errors);
        }
    }


    void validateActualVacation(PersonForm form, Errors errors) {

        BigDecimal actualVacationDays = form.getActualVacationDays();

        validateNumberNotNull(actualVacationDays, ATTRIBUTE_ACTUAL_VACATION_DAYS, errors);

        if (actualVacationDays != null) {
            BigDecimal annualVacationDays = form.getAnnualVacationDays();

            validateNumberOfDays(actualVacationDays, ATTRIBUTE_ACTUAL_VACATION_DAYS, annualVacationDays, errors);
        }
    }


    private void validateNumberNotNull(BigDecimal number, String field, Errors errors) {

        // may be that number field is null because of cast exception, than there is already a field error
        if (number == null && errors.getFieldErrors(field).isEmpty()) {
            errors.rejectValue(field, ERROR_MANDATORY_FIELD);
        }
    }


    void validateRemainingVacationDays(PersonForm form, Errors errors) {

        Settings settings = settingsService.getSettings();
        AbsenceSettings absenceSettings = settings.getAbsenceSettings();
        BigDecimal maxDays = BigDecimal.valueOf(absenceSettings.getMaximumAnnualVacationDays());

        BigDecimal remainingVacationDays = form.getRemainingVacationDays();
        BigDecimal remainingVacationDaysNotExpiring = form.getRemainingVacationDaysNotExpiring();

        validateNumberNotNull(remainingVacationDays, ATTRIBUTE_REMAINING_VACATION_DAYS, errors);
        validateNumberNotNull(remainingVacationDaysNotExpiring, ATTRIBUTE_REMAINING_VACATION_DAYS_NOT_EXPIRING, errors);

        if (remainingVacationDays != null) {
            // field entitlement's remaining vacation days
            validateNumberOfDays(remainingVacationDays, ATTRIBUTE_REMAINING_VACATION_DAYS, maxDays, errors);

            if (remainingVacationDaysNotExpiring != null) {
                validateNumberOfDays(remainingVacationDaysNotExpiring, ATTRIBUTE_REMAINING_VACATION_DAYS_NOT_EXPIRING,
                    remainingVacationDays, errors);
            }
        }
    }


    private void validateNumberOfDays(BigDecimal days, String field, BigDecimal maximumDays, Errors errors) {

        // is number of days < 0 ?
        if (days.compareTo(BigDecimal.ZERO) == -1) {
            errors.rejectValue(field, ERROR_ENTRY);
        }

        // is number of days unrealistic?
        if (days.compareTo(maximumDays) == 1) {
            errors.rejectValue(field, ERROR_ENTRY);
        }
    }


    boolean validateStringLength(String string) {

        return string.length() <= MAX_CHARS;
    }


    void validatePermissions(PersonForm personForm, Errors errors) {

        List<Role> roles = personForm.getPermissions();

        if (roles == null || roles.isEmpty()) {
            errors.rejectValue(ATTRIBUTE_PERMISSIONS, ERROR_PERMISSIONS_MANDATORY);

            return;
        }

        // if role inactive set, then only this role may be selected
        if (roles.contains(Role.INACTIVE) && roles.size() != 1) {
            errors.rejectValue(ATTRIBUTE_PERMISSIONS, ERROR_PERMISSIONS_INACTIVE_ROLE);

            return;
        }

        // user role must always be selected for active user
        if (!roles.contains(Role.INACTIVE) && !roles.contains(Role.USER)) {
            errors.rejectValue(ATTRIBUTE_PERMISSIONS, ERROR_PERMISSIONS_USER_ROLE);

            return;
        }

        validateCombinationOfRoles(roles, errors);
    }


    private void validateCombinationOfRoles(List<Role> roles, Errors errors) {

        if (roles.contains(Role.DEPARTMENT_HEAD) && (roles.contains(Role.BOSS) || roles.contains(Role.OFFICE))) {
            errors.rejectValue(ATTRIBUTE_PERMISSIONS, ERROR_PERMISSIONS_COMBINATION);

            return;
        }

        if (roles.contains(Role.SECOND_STAGE_AUTHORITY) && (roles.contains(Role.BOSS) || roles.contains(Role.OFFICE))) {
            errors.rejectValue(ATTRIBUTE_PERMISSIONS, ERROR_PERMISSIONS_COMBINATION);
        }
    }


    void validateNotifications(PersonForm personForm, Errors errors) {

        List<Role> roles = personForm.getPermissions();
        List<MailNotification> notifications = personForm.getNotifications();

        if (roles != null) {
            validateCombinationOfNotificationAndRole(roles, notifications, Role.DEPARTMENT_HEAD,
                MailNotification.NOTIFICATION_DEPARTMENT_HEAD, errors);

            validateCombinationOfNotificationAndRole(roles, notifications, Role.SECOND_STAGE_AUTHORITY,
                MailNotification.NOTIFICATION_SECOND_STAGE_AUTHORITY, errors);

            validateCombinationOfNotificationAndRole(roles, notifications, Role.BOSS,
                MailNotification.NOTIFICATION_BOSS, errors);

            validateCombinationOfNotificationAndRole(roles, notifications, Role.OFFICE,
                MailNotification.NOTIFICATION_OFFICE, errors);
        }
    }


    private void validateCombinationOfNotificationAndRole(List<Role> roles, List<MailNotification> notifications,
        Role role, MailNotification notification, Errors errors) {

        if (notifications.contains(notification) && !roles.contains(role)) {
            errors.rejectValue("notifications", ERROR_NOTIFICATIONS_COMBINATION);
        }
    }


    void validateWorkingTimes(PersonForm personForm, Errors errors) {

        if (personForm.getWorkingDays() == null || personForm.getWorkingDays().isEmpty()) {
            errors.rejectValue("workingDays", ERROR_WORKING_TIME_MANDATORY);
        }
    }
}
