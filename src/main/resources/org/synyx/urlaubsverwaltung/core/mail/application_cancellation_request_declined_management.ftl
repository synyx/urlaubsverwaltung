Hallo ${recipient.niceName},

der Stornierungsantrag von ${application.person.niceName} des genehmigten Urlaub vom ${application.startDate.format("dd.MM.yyyy")} bis ${application.endDate.format("dd.MM.yyyy")} wurde abgelehnt.

<#if (comment.text)?has_content>
Kommentar von ${comment.person.niceName} zur Ablehnung des Stornierungsantrags: ${comment.text}

</#if>
Es handelt sich um folgenden Urlaubsantrag: ${baseLinkURL}web/application/${application.id?c}
