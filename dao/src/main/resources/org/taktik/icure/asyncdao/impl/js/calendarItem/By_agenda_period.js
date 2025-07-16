function(doc) {
    if (doc.java_type === 'org.taktik.icure.entities.CalendarItem' && !doc.deleted && doc.startTime != null && doc.agendaId != null) {
        const start = doc.startTime
        const end = doc.endTime
        const value = {}
        if (doc.endTime != null) {
            value.endTime = doc.endTime
        }
        if (doc.calendarItemTypeId != null) {
            value.calendarItemTypeId = doc.calendarItemTypeId
        }
        if (doc.resourceGroup != null && doc.resourceGroup.id != null) {
            value.resourceGroupId = doc.resourceGroup.id
        }
        if (doc.availabilitiesAssignmentStrategy != null) {
            value.assignmentStrategy = doc.availabilitiesAssignmentStrategy
        }
        // TODO consider status of appointments (pre-book, canceled, ...) ; maybe should include status between agenda and time
        emit([doc.agendaId, doc.startTime], value);
        if (
            end != null
            && start >= 10000000000000
            && start <= 99991231235959
            && end >= 10000000000000
            && end <= 99991231235959
        ) {
            // Note: js doesn't do strict validation of dates
            const currEmitDate = new Date(
                Math.floor(start / 1e10),
                Math.floor((start / 1e8) % 100) - 1,
                Math.floor((start / 1e6) % 100),
                Math.floor((start / 1e4) % 100),
                Math.floor((start / 1e2) % 100),
                start % 100
            )
            const endDate = new Date(
                Math.floor(end / 1e10),
                Math.floor((end / 1e8) % 100) - 1,
                Math.floor((end / 1e6) % 100),
                Math.floor((end / 1e4) % 100),
                Math.floor((end / 1e2) % 100),
                end % 100
            )
            if (new Date(currEmitDate).setFullYear(currEmitDate.getFullYear() + 1) > endDate) {
                currEmitDate.setDate(currEmitDate.getDate() + 1)
                while (currEmitDate < endDate) {
                    const emitFuzzyDate =
                        currEmitDate.getFullYear() * 1e10
                        + (currEmitDate.getMonth() + 1) * 1e8
                        + currEmitDate.getDate() * 1e6
                        + currEmitDate.getHours() * 1e4
                        + currEmitDate.getMinutes() * 1e2
                        + currEmitDate.getSeconds()
                    emit([doc.agendaId, emitFuzzyDate], null)
                    currEmitDate.setDate(currEmitDate.getDate() + 1)
                }
            }
        }
    }
}
