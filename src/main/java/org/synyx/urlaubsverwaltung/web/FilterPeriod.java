package org.synyx.urlaubsverwaltung.web;

import org.springframework.util.Assert;
import org.synyx.urlaubsverwaltung.util.DateFormat;
import org.synyx.urlaubsverwaltung.util.DateUtil;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


/**
 * Represents a period of time to filter requests by.
 */
public class FilterPeriod {

    private LocalDate startDate;
    private LocalDate endDate;

    public FilterPeriod(String startDateAsString, String endDateAsString) {

        Assert.notNull(startDateAsString, "Start date must be given");
        Assert.notNull(endDateAsString, "End date must be given");

        int currentYear = Year.now(Clock.systemUTC()).getValue();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DateFormat.PATTERN);
        try {
            this.startDate = startDateAsString.isEmpty() ?
                DateUtil.getFirstDayOfYear(currentYear) : LocalDate.parse(startDateAsString, formatter);
            this.endDate = endDateAsString.isEmpty() ?
                DateUtil.getLastDayOfYear(currentYear) : LocalDate.parse(endDateAsString, formatter);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(exception.getMessage());
        }

        Assert.isTrue(endDate.isAfter(startDate) || endDate.isEqual(startDate), "Start date must be before end date");
    }

    public LocalDate getStartDate() {

        return startDate;
    }


    public LocalDate getEndDate() {

        return endDate;
    }


    public void setStartDate(LocalDate startDate) {

        this.startDate = startDate;
    }


    public void setEndDate(LocalDate endDate) {

        this.endDate = endDate;
    }


    public String getStartDateAsString() {

        return getStartDate().format(DateTimeFormatter.ofPattern(DateFormat.PATTERN));
    }


    public String getEndDateAsString() {

        return getEndDate().format(DateTimeFormatter.ofPattern(DateFormat.PATTERN));
    }

    @Override
    public String toString() {
        return "FilterPeriod{" +
            "startDate=" + startDate +
            ", endDate=" + endDate +
            '}';
    }
}
