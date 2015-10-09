package org.synyx.urlaubsverwaltung.core.person;

import org.apache.log4j.Logger;

import org.joda.time.DateMidnight;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.synyx.urlaubsverwaltung.core.account.domain.Account;
import org.synyx.urlaubsverwaltung.core.account.service.AccountInteractionService;
import org.synyx.urlaubsverwaltung.core.account.service.AccountService;
import org.synyx.urlaubsverwaltung.core.calendar.workingtime.WorkingTimeService;
import org.synyx.urlaubsverwaltung.core.department.Department;
import org.synyx.urlaubsverwaltung.core.department.DepartmentService;
import org.synyx.urlaubsverwaltung.core.mail.MailService;
import org.synyx.urlaubsverwaltung.security.CryptoUtil;
import org.synyx.urlaubsverwaltung.web.person.PersonForm;

import java.math.BigDecimal;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author  Aljona Murygina - murygina@synyx.de
 */
@Service
@Transactional
public class PersonInteractionServiceImpl implements PersonInteractionService {

    private static final Logger LOG = Logger.getLogger(PersonInteractionServiceImpl.class);

    private final PersonService personService;
    private final WorkingTimeService workingTimeService;
    private final AccountService accountService;
    private final AccountInteractionService accountInteractionService;
    private final DepartmentService departmentService;
    private final MailService mailService;

    @Autowired
    public PersonInteractionServiceImpl(PersonService personService, WorkingTimeService workingTimeService,
        AccountService accountService, AccountInteractionService accountInteractionService,
        DepartmentService departmentService, MailService mailService) {

        this.personService = personService;
        this.workingTimeService = workingTimeService;
        this.accountService = accountService;
        this.accountInteractionService = accountInteractionService;
        this.departmentService = departmentService;
        this.mailService = mailService;
    }

    @Override
    public Person create(PersonForm personForm) {

        Person person = personForm.generatePerson();

        try {
            KeyPair keyPair = CryptoUtil.generateKeyPair();
            person.setPrivateKey(keyPair.getPrivate().getEncoded());
            person.setPublicKey(keyPair.getPublic().getEncoded());
        } catch (NoSuchAlgorithmException ex) {
            LOG.error("An error occurred while trying to generate a key pair for the person " + person.toString(), ex);
            mailService.sendKeyGeneratingErrorNotification(personForm.getLoginName(), ex.getMessage());
        }

        personService.save(person);

        touchWorkingTime(person, personForm);

        touchAccount(person, personForm);

        LOG.info("Created: " + person.toString());

        return person;
    }


    private void touchWorkingTime(Person person, PersonForm personForm) {

        workingTimeService.touch(personForm.getWorkingDays(), personForm.getValidFrom(), person);
    }


    private void touchAccount(Person person, PersonForm personForm) {

        DateMidnight validFrom = personForm.getHolidaysAccountValidFrom();
        DateMidnight validTo = personForm.getHolidaysAccountValidTo();

        BigDecimal annualVacationDays = personForm.getAnnualVacationDays();
        BigDecimal remainingVacationDays = personForm.getRemainingVacationDays();
        BigDecimal remainingVacationDaysNotExpiring = personForm.getRemainingVacationDaysNotExpiring();

        // check if there is an existing account
        Optional<Account> account = accountService.getHolidaysAccount(validFrom.getYear(), person);

        if (account.isPresent()) {
            accountInteractionService.editHolidaysAccount(account.get(), validFrom, validTo, annualVacationDays,
                remainingVacationDays, remainingVacationDaysNotExpiring);
        } else {
            accountInteractionService.createHolidaysAccount(person, validFrom, validTo, annualVacationDays,
                remainingVacationDays, remainingVacationDaysNotExpiring);
        }
    }


    @Override
    public Person update(PersonForm personForm) {

        java.util.Optional<Person> optionalPersonToUpdate = personService.getPersonByID(personForm.getId());

        if (!optionalPersonToUpdate.isPresent()) {
            throw new IllegalArgumentException("Can not find a person for ID = " + personForm.getId());
        }

        Person personToUpdate = optionalPersonToUpdate.get();

        personForm.fillPersonAttributes(personToUpdate);

        personService.save(personToUpdate);

        touchWorkingTime(personToUpdate, personForm);

        touchAccount(personToUpdate, personForm);

        touchDepartmentHeads(personToUpdate);

        LOG.info("Updated " + personToUpdate.toString());

        return personToUpdate;
    }


    /**
     * If the updated person loses department head role, all the department head mappings must be cleaned up.
     *
     * @param  person  to check lost department head role for
     */
    private void touchDepartmentHeads(Person person) {

        if (!person.hasRole(Role.DEPARTMENT_HEAD)) {
            List<Department> departments = departmentService.getManagedDepartmentsOfDepartmentHead(person);

            for (Department department : departments) {
                List<Person> departmentHeads = department.getDepartmentHeads();

                List<Person> updatedDepartmentHeads = departmentHeads.stream().filter(departmentHead ->
                            !departmentHead.equals(person)).collect(Collectors.toList());

                if (departmentHeads.size() != updatedDepartmentHeads.size()) {
                    department.setDepartmentHeads(updatedDepartmentHeads);

                    departmentService.update(department);
                }
            }
        }
    }
}
