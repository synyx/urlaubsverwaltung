<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@taglib prefix="uv" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="asset" uri="/WEB-INF/asset.tld" %>
<%@taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>

<%@ page trimDirectiveWhitespaces="true" %>

<!DOCTYPE html>
<html lang="${language}">

<head>
    <title>
        <spring:message code="absences.overview.header.title"/>
    </title>
    <link rel="stylesheet" href="<asset:url value='absences_overview.css' />"/>
    <uv:custom-head/>
    <script defer src="<asset:url value='npm.tablesorter.js' />"></script>
    <script defer src="<asset:url value='absences_overview.js' />"></script>
</head>

<body>
<spring:url var="URL_PREFIX" value="/web"/>

<uv:menu/>

<div class="content">
    <div class="container">

        <uv:section-heading>
            <jsp:attribute name="actions">
                <uv:print/>
            </jsp:attribute>
            <jsp:body>
                <h1 id="absence">
                    <spring:message code="absences.overview.title"/>
                </h1>
            </jsp:body>
        </uv:section-heading>

        <form:form method="GET" action="${URL_PREFIX}/absences" id="absenceOverviewForm" class="print:tw-hidden">

            <div class="col-md-12">
                <div class="form-group">
                    <div class="row">
                        <label class="control-label col-md-1" for="yearSelect">
                            <spring:message code="absences.overview.year"/>:
                        </label>
                        <div class="col-md-4">
                            <uv:select id="yearSelect" name="year">
                                <c:forEach var="i" begin="1" end="9">
                                    <option
                                        value="${currentYear - 10 + i}" ${(currentYear - 10 + i) == selectedYear ? 'selected="selected"' : ''}>
                                        <c:out value="${currentYear - 10 + i}"/>
                                    </option>
                                </c:forEach>
                                <option
                                    value="${currentYear}" ${currentYear == selectedYear ? 'selected="selected"' : ''}>
                                    <c:out value="${currentYear}"/>
                                </option>
                                <option
                                    value="${currentYear + 1}" ${(currentYear + 1) == selectedYear ? 'selected="selected"' : ''}>
                                    <c:out value="${currentYear + 1}"/>
                                </option>
                            </uv:select>
                        </div>
                    </div>
                </div>
                <div class="form-group">
                    <div class="row">
                        <label class="control-label col-md-1" for="monthSelect">
                            <spring:message code="absences.overview.month"/>:
                        </label>
                        <div class="col-md-4">
                            <uv:select id="monthSelect" name="month">
                                <option value="" ${selectedMonth == '' ? 'selected="selected"' : ''}>
                                    <spring:message code="month.all"/>
                                </option>
                                <option disabled>──────────</option>
                                <c:forEach var="i" begin="1" end="12">
                                    <option value="${i}" ${i == selectedMonth ? 'selected="selected"' : ''}>
                                        <c:choose>
                                            <c:when test="${i == 1}"><spring:message code="month.january"/></c:when>
                                            <c:when test="${i == 2}"><spring:message code="month.february"/></c:when>
                                            <c:when test="${i == 3}"><spring:message code="month.march"/></c:when>
                                            <c:when test="${i == 4}"><spring:message code="month.april"/></c:when>
                                            <c:when test="${i == 5}"><spring:message code="month.may"/></c:when>
                                            <c:when test="${i == 6}"><spring:message code="month.june"/></c:when>
                                            <c:when test="${i == 7}"><spring:message code="month.july"/></c:when>
                                            <c:when test="${i == 8}"><spring:message code="month.august"/></c:when>
                                            <c:when test="${i == 9}"><spring:message code="month.september"/></c:when>
                                            <c:when test="${i == 10}"><spring:message code="month.october"/></c:when>
                                            <c:when test="${i == 11}"><spring:message code="month.november"/></c:when>
                                            <c:when test="${i == 12}"><spring:message code="month.december"/></c:when>
                                        </c:choose>
                                    </option>
                                </c:forEach>
                            </uv:select>
                        </div>
                    </div>
                </div>
                <c:if test="${not empty departments}">
                    <div class="form-group">
                        <div class="row">
                            <label class="control-label col-md-1" for="departmentSelect">
                                <spring:message code="absences.overview.department"/>:
                            </label>
                            <div class="col-md-4">
                                <uv:multi-select id="departmentSelect" name="department">
                                    <c:forEach items="${departments}" var="department">
                                        <uv:multi-select-item value="${department.name}"
                                                              selected="${selectedDepartments.contains(department.name)}">
                                            <c:out value="${department.name}"/>
                                        </uv:multi-select-item>
                                    </c:forEach>
                                </uv:multi-select>
                            </div>
                            <div class="col-md-5 hidden-xs">
                                <span class="help-block tw-text-sm">
                                    <icon:information-circle className="tw-w-4 tw-h-4" solid="true"/>
                                    <spring:message code="absences.overview.department.help"/>
                                </span>
                            </div>
                        </div>
                    </div>
                </c:if>
            </div>
        </form:form>

        <div class="row">
            <div class="col-xs-12">
                <hr class="print:tw-hidden"/>

                <c:forEach items="${absenceOverview.months}" var="month">
                    <div class="tw-mb-10 print:tw-no-break-inside">
                        <h2 id="absence-table-${month.nameOfMonth}"
                            class="tw-text-2xl tw-m-0 tw-mb-5 print:tw-mb-1 ${fn:length(absenceOverview.months) == 1 ? 'tw-hidden print:tw-block' : ''}">
                            <c:out value="${month.nameOfMonth}"/>
                            <span class="hidden print:tw-inline"> <c:out value="${selectedYear}"/></span>
                        </h2>
                        <table class="sortable vacationOverview-table tw-text-sm" role="grid"
                               aria-describedby="absence-table-${month.nameOfMonth}">
                            <thead>
                            <tr>
                                <th scope="col" class="sortable-field">&nbsp;</th>
                                <th scope="col" class="sortable-field">&nbsp;</th>
                                <c:forEach items="${month.days}" var="day">
                                    <th scope="col"
                                        class="non-sortable text-gray-700 ${day.weekend ? 'vacationOverview-day-weekend' : ''}
                                                ${(day.type eq 'publicHolidayFull') ? ' vacationOverview-day-public-holiday' : ''}
                                                ${(day.type eq 'publicHolidayMorning') ? ' vacationOverview-day-public-holiday-half-day-morning-headline' : ''}
                                                ${(day.type eq 'publicHolidayNoon') ? ' vacationOverview-day-public-holiday-half-day-noon-headline' : ''}"
                                    >
                                        <c:out value="${day.dayOfMonth}"/>
                                    </th>
                                </c:forEach>
                            </tr>
                            </thead>
                            <tbody class="vacationOverview-tbody">
                            <c:forEach var="person" items="${month.persons}">
                                <tr role="row">
                                    <th scope="row"><c:out value="${person.firstName}"/></th>
                                    <th scope="row"><c:out value="${person.lastName}"/></th>
                                    <c:forEach var="absence" items="${person.days}">
                                        <td class="vacationOverview-day ${(absence.type eq '') ? 'vacationOverview-day-item' : ''}
                                                ${(absence.type eq 'absenceFull') ? ' vacationOverview-day-absence-status' : ''}
                                                ${(absence.type eq 'absenceMorning') ? ' vacationOverview-day-absence-status-morning' : ''}
                                                ${(absence.type eq 'absenceNoon') ? ' vacationOverview-day-absence-status-noon' : ''}
                                                ${(absence.type eq 'waitingVacationFull') ? ' vacationOverview-day-personal-holiday-status-WAITING' : ''}
                                                ${(absence.type eq 'waitingVacationMorning') ? ' vacationOverview-day-personal-holiday-half-day-status-WAITING-morning' : ''}
                                                ${(absence.type eq 'waitingVacationNoon') ? ' vacationOverview-day-personal-holiday-half-day-status-WAITING-noon' : ''}
                                                ${(absence.type eq 'allowedVacationFull') ? ' vacationOverview-day-personal-holiday-status-ALLOWED' : ''}
                                                ${(absence.type eq 'allowedVacationMorning') ? ' vacationOverview-day-personal-holiday-half-day-status-ALLOWED-morning' : ''}
                                                ${(absence.type eq 'allowedVacationNoon') ? ' vacationOverview-day-personal-holiday-half-day-status-ALLOWED-noon' : ''}
                                                ${(absence.type eq 'activeSickNoteFull') ? ' vacationOverview-day-sick-note' : ''}
                                                ${(absence.type eq 'activeSickNoteMorning') ? ' vacationOverview-day-sick-note-half-day-morning' : ''}
                                                ${(absence.type eq 'activeSickNoteNoon') ? ' vacationOverview-day-sick-note-half-day-noon' : ''}
                                                ${(absence.type eq 'publicHolidayFull') ? ' vacationOverview-day-public-holiday' : ''}
                                                ${(absence.type eq 'publicHolidayMorning') ? ' vacationOverview-day-public-holiday-half-day-morning' : ''}
                                                ${(absence.type eq 'publicHolidayNoon') ? ' vacationOverview-day-public-holiday-half-day-noon' : ''}
                                                ${(absence.weekend) ? ' vacationOverview-day-weekend' : ''}"
                                        >
                                            <span class="tw-hidden print:tw-inline print:tw-font-mono">
                                                <c:if test="${absence.type eq 'waitingVacationFull'}">
                                                    <spring:message code="absences.overview.vacation.abbr"/>
                                                </c:if>
                                                <c:if test="${absence.type eq 'waitingVacationMorning'}">
                                                    <spring:message code="absences.overview.vacation.noon.abbr"/>
                                                </c:if>
                                                <c:if test="${absence.type eq 'waitingVacationNoon'}">
                                                    <spring:message code="absences.overview.vacation.noon.abbr"/>
                                                </c:if>
                                                <c:if test="${absence.type eq 'allowedVacationFull'}">
                                                    <spring:message code="absences.overview.allowed.abbr"/>
                                                </c:if>
                                                <c:if test="${absence.type eq 'allowedVacationMorning'}">
                                                    <spring:message code="absences.overview.allowed.morning.abbr"/>
                                                </c:if>
                                                <c:if test="${absence.type eq 'allowedVacationNoon'}">
                                                    <spring:message code="absences.overview.allowed.noon.abbr"/>
                                                </c:if>
                                                <c:if test="${absence.type eq 'activeSickNoteFull'}">
                                                    <spring:message code="absences.overview.sick.abbr"/>
                                                </c:if>
                                                <c:if test="${absence.type eq 'activeSickNoteMorning'}">
                                                    <spring:message code="absences.overview.sick.morning.abbr"/>
                                                </c:if>
                                                <c:if test="${absence.type eq 'activeSickNoteNoon'}">
                                                    <spring:message code="absences.overview.sick.noon.abbr"/>
                                                </c:if>
                                            </span>
                                        </td>
                                    </c:forEach>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:forEach>
            </div>
        </div>

        <div id="vacationOverviewLegend" class="row tw-mb-8 print:tw-no-break-inside">
            <div class="col-md-10">
                <table aria-hidden="true" class="vacationOverview-table-legend tw-text-sm print:tw-font-mono">
                    <caption>
                        <spring:message code="absences.overview.legendTitle"/>
                    </caption>
                    <tbody>
                    <tr>
                        <td class='vacationOverview-legend-colorbox vacationOverview-day-weekend'></td>
                        <td class='vacationOverview-legend-text'>
                            <spring:message code="absences.overview.weekend"/>
                        </td>
                    </tr>
                    <c:choose>
                        <c:when test="${isPrivileged}">
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-personal-holiday-status-ALLOWED'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.allowed.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.allowed"/>
                                </td>
                            </tr>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-personal-holiday-half-day-status-ALLOWED-morning'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.allowed.morning.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.allowed.morning"/>
                                </td>
                            </tr>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-personal-holiday-half-day-status-ALLOWED-noon'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.allowed.noon.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.allowed.noon"/>
                                </td>
                            </tr>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-personal-holiday-status-WAITING'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.vacation.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.vacation"/>
                                </td>
                            </tr>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-personal-holiday-half-day-status-WAITING-morning'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.vacation.morning.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.vacation.morning"/>
                                </td>
                            </tr>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-personal-holiday-half-day-status-WAITING-noon'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.vacation.noon.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.vacation.noon"/>
                                </td>
                            </tr>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-sick-note'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.sick.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.sick"/>
                                </td>
                            </tr>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-sick-note-half-day-morning'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.sick.morning.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.sick.morning"/>
                                </td>
                            </tr>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-sick-note-half-day-noon'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.sick.noon.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.sick.noon"/>
                                </td>
                            </tr>
                        </c:when>
                        <c:otherwise>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-absence-status'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.absence.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.absence"/>
                                </td>
                            </tr>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-absence-status-morning'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.absence.morning.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.absence.morning"/>
                                </td>
                            </tr>
                            <tr>
                                <td class='vacationOverview-legend-colorbox vacationOverview-day-absence-status-noon'>
                                    <span class="tw-hidden print:tw-inline">
                                        <spring:message code="absences.overview.absence.noon.abbr"/>
                                    </span>
                                </td>
                                <td class='vacationOverview-legend-text'>
                                    <spring:message code="absences.overview.absence.noon"/>
                                </td>
                            </tr>
                        </c:otherwise>
                    </c:choose>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

</div>

</body>
