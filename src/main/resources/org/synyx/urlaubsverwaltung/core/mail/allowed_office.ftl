Hallo Office,

es liegt ein neuer genehmigter Antrag vor: ${settings.baseLinkURL}web/application/<#if application.id??>${application.id}</#if>

<#if (comment.text)??>
Kommentar von ${comment.person.niceName} zum Antrag: ${comment.text}

</#if>
----------------------------------------------------------------------------------------------

Informationen zum Urlaubsantrag:

Mitarbeiter: ${application.person.niceName}
Antragsdatum: ${application.applicationDate.toString("dd.MM.yyyy")}
Zeitraum des beantragten Urlaubs: ${application.startDate.toString("dd.MM.yyyy")} bis ${application.endDate.toString("dd.MM.yyyy")}, ${dayLength}
Art des Urlaubs: ${application.vacationType.displayName}
<#if (application.reason)??>
Grund: ${application.reason}
</#if>
<#if (application.holidayReplacement.niceName)??>
Vertreter: ${application.holidayReplacement.niceName}
</#if>
<#if (application.address)??>
Anschrift/Telefon während des Urlaubs: ${application.address}
</#if>
