package org.synyx.urlaubsverwaltung.account.settings;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.synyx.urlaubsverwaltung.account.AccountProperties;

import static org.synyx.urlaubsverwaltung.security.SecurityRules.IS_OFFICE;

@Controller
@RequestMapping("/web/account/settings")
public class AccountSettingsController {

    private final AccountSettingsService accountSettingsService;
    private final AccountProperties accountProperties;

    public AccountSettingsController(AccountSettingsService accountSettingsService, AccountProperties accountProperties) {
        this.accountSettingsService = accountSettingsService;
        this.accountProperties = accountProperties;
    }

    @GetMapping
    @PreAuthorize(IS_OFFICE)
    public String getAccountSettings(Model model) {

        model.addAttribute("defaultVacationDaysFromSettings", accountProperties.getDefaultVacationDays() == -1);

        AccountSettingsDto settingsDto = accountSettingsService.getSettingsDto();
        model.addAttribute("accountSettings", settingsDto);

        return "account/account_settings";
    }

    @PostMapping
    @PreAuthorize(IS_OFFICE)
    public String saveAccountSettings(@ModelAttribute("accountSettings") AccountSettingsDto accountSettingsDto,
                                      Model model) {

        model.addAttribute("defaultVacationDaysFromSettings", accountProperties.getDefaultVacationDays() == -1);

        accountSettingsService.save(accountSettingsDto);
        model.addAttribute("accountSettings", accountSettingsDto);
        model.addAttribute("success", true);

        return "account/account_settings";
    }
}
