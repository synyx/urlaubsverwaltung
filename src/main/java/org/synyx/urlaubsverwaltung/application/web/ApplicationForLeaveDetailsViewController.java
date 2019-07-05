package org.synyx.urlaubsverwaltung.application.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.synyx.urlaubsverwaltung.account.domain.Account;
import org.synyx.urlaubsverwaltung.account.service.AccountService;
import org.synyx.urlaubsverwaltung.account.service.VacationDaysService;
import org.synyx.urlaubsverwaltung.application.domain.Application;
import org.synyx.urlaubsverwaltung.application.domain.ApplicationComment;
import org.synyx.urlaubsverwaltung.application.service.ApplicationCommentService;
import org.synyx.urlaubsverwaltung.application.service.ApplicationInteractionService;
import org.synyx.urlaubsverwaltung.application.service.ApplicationService;
import org.synyx.urlaubsverwaltung.application.service.exception.ImpatientAboutApplicationForLeaveProcessException;
import org.synyx.urlaubsverwaltung.application.service.exception.RemindAlreadySentException;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.person.UnknownPersonException;
import org.synyx.urlaubsverwaltung.security.SecurityRules;
import org.synyx.urlaubsverwaltung.util.DateUtil;
import org.synyx.urlaubsverwaltung.web.ControllerConstants;
import org.synyx.urlaubsverwaltung.workingtime.WorkDaysService;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTime;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.time.ZoneOffset.UTC;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.ALLOWED;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.TEMPORARY_ALLOWED;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.WAITING;
import static org.synyx.urlaubsverwaltung.person.Role.BOSS;
import static org.synyx.urlaubsverwaltung.person.Role.DEPARTMENT_HEAD;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.SECOND_STAGE_AUTHORITY;
import static org.synyx.urlaubsverwaltung.web.ControllerConstants.ERRORS_ATTRIBUTE;


/**
 * Controller to manage applications for leave.
 */
@RequestMapping("/web/application")
@Controller
public class ApplicationForLeaveDetailsViewController {

    private static final String BEFORE_APRIL_ATTRIBUTE = "beforeApril";

    private final PersonService personService;
    private final AccountService accountService;
    private final ApplicationService applicationService;
    private final ApplicationInteractionService applicationInteractionService;
    private final VacationDaysService vacationDaysService;
    private final ApplicationCommentService commentService;
    private final WorkDaysService workDaysService;
    private final ApplicationCommentValidator commentValidator;
    private final DepartmentService departmentService;
    private final WorkingTimeService workingTimeService;

    @Autowired
    public ApplicationForLeaveDetailsViewController(VacationDaysService vacationDaysService, PersonService personService,
                                                    AccountService accountService, ApplicationService applicationService,
                                                    ApplicationInteractionService applicationInteractionService,
                                                    ApplicationCommentService commentService, WorkDaysService workDaysService,
                                                    ApplicationCommentValidator commentValidator,
                                                    DepartmentService departmentService, WorkingTimeService workingTimeService) {
        this.vacationDaysService = vacationDaysService;
        this.personService = personService;
        this.accountService = accountService;
        this.applicationService = applicationService;
        this.applicationInteractionService = applicationInteractionService;
        this.commentService = commentService;
        this.workDaysService = workDaysService;
        this.commentValidator = commentValidator;
        this.departmentService = departmentService;
        this.workingTimeService = workingTimeService;
    }

    @GetMapping("/{applicationId}")
    public String showApplicationDetail(@PathVariable("applicationId") Integer applicationId,
                                        @RequestParam(value = ControllerConstants.YEAR_ATTRIBUTE, required = false) Integer requestedYear,
                                        @RequestParam(value = "action", required = false) String action,
                                        @RequestParam(value = "shortcut", required = false) boolean shortcut, Model model)
        throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId).orElseThrow(() ->
            new UnknownApplicationForLeaveException(applicationId));

        final Person signedInUser = personService.getSignedInUser();
        final Person person = application.getPerson();

        if (!departmentService.isSignedInUserAllowedToAccessPersonData(signedInUser, person)) {
            throw new AccessDeniedException(format("User '%s' has not the correct permissions to see application for " +
                "leave of user '%s'", signedInUser.getId(), person.getId()));
        }

        final int year = requestedYear == null ? application.getEndDate().getYear() : requestedYear;

        prepareDetailView(application, year, action, shortcut, model);

        return "application/app_detail";
    }


    private void prepareDetailView(Application application, int year, String action, boolean shortcut, Model model) {

        // COMMENTS
        final List<ApplicationComment> comments = commentService.getCommentsByApplication(application);

        model.addAttribute("comment", new ApplicationCommentForm());
        model.addAttribute("comments", comments);
        model.addAttribute("lastComment", comments.get(comments.size() - 1));

        // SPECIAL ATTRIBUTES FOR BOSSES / DEPARTMENT HEADS
        final Person signedInUser = personService.getSignedInUser();

        final boolean isNotYetAllowed = application.hasStatus(WAITING) || application.hasStatus(TEMPORARY_ALLOWED);
        final boolean isPrivilegedUser = signedInUser.hasRole(BOSS) || signedInUser.hasRole(DEPARTMENT_HEAD)
            || signedInUser.hasRole(SECOND_STAGE_AUTHORITY);

        if (isNotYetAllowed && isPrivilegedUser) {
            model.addAttribute("bosses", personService.getActivePersonsByRole(BOSS));
            model.addAttribute("referredPerson", new ReferredPerson());
        }

        // APPLICATION FOR LEAVE
        model.addAttribute("application", new ApplicationForLeave(application, workDaysService));

        // WORKING TIME FOR VACATION PERIOD
        Optional<WorkingTime> optionalWorkingTime = workingTimeService.getByPersonAndValidityDateEqualsOrMinorDate(
            application.getPerson(), application.getStartDate());

        optionalWorkingTime.ifPresent(workingTime -> model.addAttribute("workingTime", workingTime));

        // DEPARTMENT APPLICATIONS FOR LEAVE
        List<Application> departmentApplications =
            departmentService.getApplicationsForLeaveOfMembersInDepartmentsOfPerson(application.getPerson(),
                application.getStartDate(), application.getEndDate());
        model.addAttribute("departmentApplications", departmentApplications);

        // HOLIDAY ACCOUNT
        Optional<Account> account = accountService.getHolidaysAccount(year, application.getPerson());

        if (account.isPresent()) {
            final Account acc = account.get();
            final Optional<Account> accountNextYear = accountService.getHolidaysAccount(year + 1, application.getPerson());
            model.addAttribute("vacationDaysLeft", vacationDaysService.getVacationDaysLeft(account.get(), accountNextYear));
            model.addAttribute("account", acc);
            model.addAttribute(BEFORE_APRIL_ATTRIBUTE, DateUtil.isBeforeApril(LocalDate.now(UTC), acc.getYear()));
        }

        // UNSPECIFIC ATTRIBUTES
        model.addAttribute(ControllerConstants.YEAR_ATTRIBUTE, year);
        model.addAttribute("action", action);
        model.addAttribute("shortcut", shortcut);
    }


    /*
     * Allow a not yet allowed application for leave (Privileged user only!).
     */
    @PreAuthorize(SecurityRules.IS_BOSS_OR_DEPARTMENT_HEAD_OR_SECOND_STAGE_AUTHORITY)
    @PostMapping("/{applicationId}/allow")
    public String allowApplication(@PathVariable("applicationId") Integer applicationId,
                                   @ModelAttribute("comment") ApplicationCommentForm comment,
                                   @RequestParam(value = "redirect", required = false) String redirectUrl, Errors errors,
                                   RedirectAttributes redirectAttributes) throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId).orElseThrow(() ->
            new UnknownApplicationForLeaveException(applicationId));

        final Person signedInUser = personService.getSignedInUser();
        final Person person = application.getPerson();

        final boolean isBoss = signedInUser.hasRole(BOSS);
        final boolean isDepartmentHead = signedInUser.hasRole(DEPARTMENT_HEAD)
            && departmentService.isDepartmentHeadOfPerson(signedInUser, person);
        final boolean isSecondStageAuthority = signedInUser.hasRole(SECOND_STAGE_AUTHORITY)
            && departmentService.isSecondStageAuthorityOfPerson(signedInUser, person);

        if (!isBoss && !isDepartmentHead && !isSecondStageAuthority) {
            throw new AccessDeniedException(format(
                "User '%s' has not the correct permissions to allow application for leave of user '%s'",
                signedInUser.getId(), person.getId()));
        }

        comment.setMandatory(false);
        commentValidator.validate(comment, errors);

        if (errors.hasErrors()) {
            redirectAttributes.addFlashAttribute(ERRORS_ATTRIBUTE, errors);

            return "redirect:/web/application/" + applicationId + "?action=allow";
        }

        final Application allowedApplicationForLeave = applicationInteractionService.allow(application, signedInUser,
            Optional.ofNullable(comment.getText()));

        if (allowedApplicationForLeave.hasStatus(ALLOWED)) {
            redirectAttributes.addFlashAttribute("allowSuccess", true);
        } else if (allowedApplicationForLeave.hasStatus(TEMPORARY_ALLOWED)) {
            redirectAttributes.addFlashAttribute("temporaryAllowSuccess", true);
        }

        if (redirectUrl != null) {
            return "redirect:" + redirectUrl;
        }

        return "redirect:/web/application/" + applicationId;
    }


    /*
     * If a boss is not sure about the decision if an application should be allowed or rejected, he can ask another boss
     * to decide about this application (an email is sent).
     */
    @PreAuthorize(SecurityRules.IS_BOSS_OR_DEPARTMENT_HEAD)
    @PostMapping("/{applicationId}/refer")
    public String referApplication(@PathVariable("applicationId") Integer applicationId,
                                   @ModelAttribute("referredPerson") ReferredPerson referredPerson, RedirectAttributes redirectAttributes)
        throws UnknownApplicationForLeaveException, UnknownPersonException {

        final Application application = applicationService.getApplicationById(applicationId).orElseThrow(() ->
            new UnknownApplicationForLeaveException(applicationId));

        final String referUsername = referredPerson.getUsername();
        final Person recipient = personService.getPersonByUsername(referUsername).orElseThrow(() ->
            new UnknownPersonException(referUsername));

        final Person sender = personService.getSignedInUser();

        final boolean isBoss = sender.hasRole(BOSS);
        final boolean isDepartmentHead = departmentService.isDepartmentHeadOfPerson(sender, application.getPerson());

        if (isBoss || isDepartmentHead) {
            applicationInteractionService.refer(application, recipient, sender);

            redirectAttributes.addFlashAttribute("referSuccess", true);

            return "redirect:/web/application/" + applicationId;
        }

        throw new AccessDeniedException(format(
            "User '%s' has not the correct permissions to refer application for leave to user '%s'",
            sender.getId(), referUsername));
    }


    @PreAuthorize(SecurityRules.IS_BOSS_OR_DEPARTMENT_HEAD_OR_SECOND_STAGE_AUTHORITY)
    @PostMapping("/{applicationId}/reject")
    public String rejectApplication(@PathVariable("applicationId") Integer applicationId,
                                    @ModelAttribute("comment") ApplicationCommentForm comment,
                                    @RequestParam(value = "redirect", required = false) String redirectUrl, Errors errors,
                                    RedirectAttributes redirectAttributes) throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId).orElseThrow(() ->
            new UnknownApplicationForLeaveException(applicationId));

        final Person person = application.getPerson();
        final Person signedInUser = personService.getSignedInUser();

        final boolean isBoss = signedInUser.hasRole(BOSS);
        final boolean isDepartmentHead = departmentService.isDepartmentHeadOfPerson(signedInUser, person);
        final boolean isSecondStageAuthority = departmentService.isSecondStageAuthorityOfPerson(signedInUser, person);

        if (isBoss || isDepartmentHead || isSecondStageAuthority) {
            comment.setMandatory(true);
            commentValidator.validate(comment, errors);

            if (errors.hasErrors()) {
                redirectAttributes.addFlashAttribute(ERRORS_ATTRIBUTE, errors);

                if (redirectUrl != null) {
                    return "redirect:/web/application/" + applicationId + "?action=reject&shortcut=true";
                }

                return "redirect:/web/application/" + applicationId + "?action=reject";
            }

            applicationInteractionService.reject(application, signedInUser, Optional.ofNullable(comment.getText()));
            redirectAttributes.addFlashAttribute("rejectSuccess", true);

            if (redirectUrl != null) {
                return "redirect:" + redirectUrl;
            }

            return "redirect:/web/application/" + applicationId;
        }

        throw new AccessDeniedException(format("User '%s' has not the correct permissions to reject application for " +
            "leave of user '%s'", signedInUser.getId(), person.getId()));
    }


    /*
     * Cancel an application for leave. Cancelling an application for leave on behalf for someone is allowed only for
     * Office.
     */
    @PostMapping("/{applicationId}/cancel")
    public String cancelApplication(@PathVariable("applicationId") Integer applicationId,
                                    @ModelAttribute("comment") ApplicationCommentForm comment, Errors errors, RedirectAttributes redirectAttributes)
        throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId).orElseThrow(() ->
            new UnknownApplicationForLeaveException(applicationId));

        final Person signedInUser = personService.getSignedInUser();

        final boolean isAllowed = application.hasStatus(ALLOWED);
        final boolean isWaiting = application.hasStatus(WAITING);
        final boolean isTemporaryAllowed = application.hasStatus((TEMPORARY_ALLOWED));

        // security check: only two cases where cancelling is possible
        // 1: user can cancel her own applications for leave if it has not been allowed yet
        // 2: user can request cancellation if the application is already allowed.
        // 3: office can cancel all applications for leave that has the state waiting or allowed, even for other persons
        if (isWaiting && signedInUser.equals(application.getPerson())) {
            // user can cancel only her own waiting applications, so the comment is NOT mandatory
            comment.setMandatory(false);
        } else if ((isWaiting || isAllowed || isTemporaryAllowed) && signedInUser.hasRole(OFFICE)) {
            // office cancels application of other users, state can be waiting or allowed, so the comment is mandatory
            comment.setMandatory(true);
        } else {
            throw new AccessDeniedException(format("User '%s' has not the correct permissions to cancel application for " +
                "leave of user '%s'", signedInUser.getId(), application.getPerson().getId()));
        }

        commentValidator.validate(comment, errors);

        if (errors.hasErrors()) {
            redirectAttributes.addFlashAttribute(ERRORS_ATTRIBUTE, errors);

            return "redirect:/web/application/" + applicationId + "?action=cancel";
        }

        applicationInteractionService.cancel(application, signedInUser, Optional.ofNullable(comment.getText()));

        return "redirect:/web/application/" + applicationId;
    }


    /*
     * Remind the bosses about the decision of an application for leave.
     */
    @PostMapping("/{applicationId}/remind")
    public String remindBoss(@PathVariable("applicationId") Integer applicationId,
                             RedirectAttributes redirectAttributes) throws UnknownApplicationForLeaveException {

        final Application application = applicationService.getApplicationById(applicationId).orElseThrow(() ->
            new UnknownApplicationForLeaveException(applicationId));

        try {
            applicationInteractionService.remind(application);
            redirectAttributes.addFlashAttribute("remindIsSent", true);
        } catch (RemindAlreadySentException ex) {
            redirectAttributes.addFlashAttribute("remindAlreadySent", true);
        } catch (ImpatientAboutApplicationForLeaveProcessException ex) {
            redirectAttributes.addFlashAttribute("remindNoWay", true);
        }

        return "redirect:/web/application/" + applicationId;
    }
}
