<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="spring" uri="http://www.springframework.org/tags" %>

<%@attribute name="duration" type="java.time.Duration" required="true" %>

<%@ tag trimDirectiveWhitespaces="true" %>

<c:if test="${duration.negative}">-</c:if>
<c:if test="${duration.abs().toHours() > 0}">
    <c:out value="${duration.abs().toHours()} "/><spring:message code="hours.abbr"/>
</c:if>
<c:if test="${duration.abs().toMinutesPart() > 0}">
    <c:if test="${duration.abs().toHours() > 0}">
        <c:out value=" "/>
    </c:if>
    <c:out value="${duration.abs().toMinutesPart()} "/><spring:message code="minutes.abbr"/>
</c:if>
<c:if test="${duration.toMinutes() == 0}">
    <spring:message code="overtime.person.zero"/>
</c:if>

