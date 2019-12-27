package outage.atco.outageandroid.models.outages

import outage.atco.outageandroid.utility.OutageBoundariesQueryFeature
import java.util.*

class PlannedOutage: BaseOutage {
    var descriptionOfWork: String?      = null

    constructor(data: Any): this() {

        if (data is OutageBoundariesQueryFeature) {
            val attributes = data.attributes

            outageID = attributes.OBJECTID.toString()
            startDate = Date(attributes.STARTDATE.toLong())
            finishDate = Date(attributes.ENDDATE.toLong())
            customersAffected = attributes.NUMOFCUSTOMEREFFECTED
            status = attributes.STATUS

        } else {
            throw Error("Cannot instantiate Planned Outage")
        }

    }

    constructor() : super()
}