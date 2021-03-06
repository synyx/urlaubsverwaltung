<%@page contentType="text/html" pageEncoding="UTF-8" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@taglib prefix="uv" tagdir="/WEB-INF/tags" %>
<%@taglib prefix="icon" tagdir="/WEB-INF/tags/icons" %>
<%@taglib prefix="asset" uri="/WEB-INF/asset.tld" %>

<!DOCTYPE html>
<html lang="${language}">
<head>
    <title>
        <spring:message code="overtime.header.title" arguments="${person.niceName}"/>
    </title>
    <uv:custom-head/>
    <script defer src="<asset:url value='overtime_overview.js' />"></script>
</head>
<body>

<spring:url var="URL_PREFIX" value="/web"/>

<uv:menu/>

<div class="content">
    <div class="container">
        <div class="row tw-mb-2">
            <div class="col-xs-12">
                <uv:section-heading>
                    <jsp:attribute name="actions">
                        <c:if test="${userIsAllowedToWriteOvertime}">
                            <a href="${URL_PREFIX}/overtime/new?person=${person.id}" class="icon-link tw-px-1" data-title="<spring:message code="action.overtime.new"/>">
                                <icon:plus-circle className="tw-w-5 tw-h-5" />
                            </a>
                        </c:if>
                    </jsp:attribute>
                    <jsp:body>
                        <h1>
                            <spring:message code="overtime.title"/>
                        </h1>
                        <uv:year-selector year="${year}" hrefPrefix="${URL_PREFIX}/overtime?person=${person.id}&year="/>
                    </jsp:body>
                </uv:section-heading>
            </div>
        </div>
        <div class="row lg:tw-mb-8">
            <div class="sm:tw-flex">
                <div class="sm:tw-flex-1 lg:tw-flex-none lg:tw-w-1/3">
                    <uv:person person="${person}" cssClass="tw-h-24 lg:tw-h-32 tw-border-none"/>
                </div>
                <div class="sm:tw-flex-1 lg:tw-flex">
                    <div class="tw-flex-1">
                        <uv:overtime-total hours="${overtimeTotal}" cssClass="tw-h-32 tw-items-center tw-border-none"/>
                    </div>
                    <div class="tw-flex-1">
                        <uv:overtime-left hours="${overtimeLeft}" cssClass="tw-h-32 tw-items-center tw-border-none"/>
                    </div>
                </div>
            </div>
        </div>
        <div class="row">
            <div class="col-xs-12">
                <c:choose>
                    <c:when test="${empty records}">
                        <p class="tw-text-center tw-mt-4 lg:tw-mt-8"><spring:message code="overtime.none"/></p>
                    </c:when>
                    <c:otherwise>
                        <table class="list-table selectable-table tw-text-sm">
                            <caption class="tw-sr-only"><spring:message code="overtime.list.title"/></caption>
                            <thead class="tw-sr-only">
                                <tr>
                                    <th scope="col"><spring:message code="overtime.list.col.icon"/></th>
                                    <th scope="col"><spring:message code="overtime.list.col.date"/></th>
                                    <th scope="col"><spring:message code="overtime.list.col.duration"/></th>
                                    <th scope="col"><spring:message code="overtime.list.col.last-edited"/></th>
                                </tr>
                            </thead>
                            <tbody>
                            <c:forEach items="${records}" var="record">
                                <tr onclick="navigate('${URL_PREFIX}/overtime/${record.id}');">
                                    <td class="is-centered state">
                                        <span class="print:tw-hidden">
                                            <icon:briefcase className="tw-w-6 tw-h-6"/>
                                        </span>
                                    </td>
                                    <td>
                                        <h4 class="visible-print">
                                            <spring:message code="overtime.title"/>
                                        </h4>
                                        <a class="print:tw-hidden" href="${URL_PREFIX}/overtime/${record.id}">
                                            <h4><spring:message code="overtime.title"/></h4>
                                        </a>
                                        <p>
                                            <uv:date-range from="${record.startDate}" to="${record.endDate}" />
                                        </p>
                                    </td>
                                    <td class="is-centered">
                                        <uv:duration duration="${record.duration}"/>
                                    </td>
                                    <td class="print:tw-hidden is-centered hidden-xs">
                                        <div class="tw-flex tw-items-center tw-justify-end">
                                            <icon:clock className="tw-w-4 tw-h-4"/>
                                            &nbsp;<spring:message code="overtime.progress.lastEdited"/>
                                            <uv:date date="${record.lastModificationDate}"/>
                                        </div>
                                    </td>
                                </tr>
                            </c:forEach>
                            </tbody>
                        </table>
                    </c:otherwise>
                </c:choose>
            </div>

        </div>
        <%-- End of row --%>
    </div>
    <%-- End of container --%>
</div>
<%-- End of content --%>

</body>
</html>
