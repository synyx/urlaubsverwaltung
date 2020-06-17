package org.synyx.urlaubsverwaltung.account.web;

import org.synyx.urlaubsverwaltung.account.domain.Account;
import org.synyx.urlaubsverwaltung.util.DateUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AccountForm {

    private int holidaysAccountYear;

    private LocalDate holidaysAccountValidFrom;

    private LocalDate holidaysAccountValidTo;

    private BigDecimal annualVacationDays;

    private BigDecimal actualVacationDays;

    private BigDecimal remainingVacationDays;

    private BigDecimal remainingVacationDaysNotExpiring;

    private String comment;

    private AccountForm() {
    }

    AccountForm(int year) {

        this.holidaysAccountYear = year;
        this.holidaysAccountValidFrom = DateUtil.getFirstDayOfYear(year);
        this.holidaysAccountValidTo = DateUtil.getLastDayOfYear(year);
    }

    AccountForm(Account holidaysAccountOptional) {

        this.holidaysAccountYear = holidaysAccountOptional.getValidFrom().getYear();
        this.holidaysAccountValidFrom = holidaysAccountOptional.getValidFrom();
        this.holidaysAccountValidTo = holidaysAccountOptional.getValidTo();
        this.annualVacationDays = holidaysAccountOptional.getAnnualVacationDays();
        this.actualVacationDays = holidaysAccountOptional.getVacationDays();
        this.remainingVacationDays = holidaysAccountOptional.getRemainingVacationDays();
        this.remainingVacationDaysNotExpiring = holidaysAccountOptional.getRemainingVacationDaysNotExpiring();
        this.comment = holidaysAccountOptional.getComment();
    }

    public int getHolidaysAccountYear() {

        return holidaysAccountYear;
    }

    public void setHolidaysAccountYear(int holidaysAccountYear) {

        this.holidaysAccountYear = holidaysAccountYear;
    }

    public LocalDate getHolidaysAccountValidFrom() {

        return holidaysAccountValidFrom;
    }

    public void setHolidaysAccountValidFrom(LocalDate holidaysAccountValidFrom) {

        this.holidaysAccountValidFrom = holidaysAccountValidFrom;
    }

    public LocalDate getHolidaysAccountValidTo() {

        return holidaysAccountValidTo;
    }

    public void setHolidaysAccountValidTo(LocalDate holidaysAccountValidTo) {

        this.holidaysAccountValidTo = holidaysAccountValidTo;
    }

    public BigDecimal getAnnualVacationDays() {

        return annualVacationDays;
    }

    public void setAnnualVacationDays(BigDecimal annualVacationDays) {

        this.annualVacationDays = annualVacationDays;
    }

    public BigDecimal getActualVacationDays() {

        return actualVacationDays;
    }

    public void setActualVacationDays(BigDecimal actualVacationDays) {

        this.actualVacationDays = actualVacationDays;
    }

    public BigDecimal getRemainingVacationDays() {

        return remainingVacationDays;
    }

    public void setRemainingVacationDays(BigDecimal remainingVacationDays) {

        this.remainingVacationDays = remainingVacationDays;
    }

    public BigDecimal getRemainingVacationDaysNotExpiring() {

        return remainingVacationDaysNotExpiring;
    }

    public void setRemainingVacationDaysNotExpiring(BigDecimal remainingVacationDaysNotExpiring) {

        this.remainingVacationDaysNotExpiring = remainingVacationDaysNotExpiring;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
