package outage.atco.outageandroid.models.outages

import outage.atco.outageandroid.utility.OutageFeature
import java.util.*


class ActiveOutage: BaseOutage {
    var objectID: Int?                  = null
    private var updateDTS: Double?              = null
    var outageTypeDesc: String?         = null
    var causeCode: String?              = null
    private var supplemental1: String?          = null
    var siteID: String?                 = null
    var custNameLabel: String?          = null
    var load: Int?                      = null
    var siteIDExcludeReason: String?    = null
    var customersLocationName: String?  = null

    private val unknownString                   = "Unknown"

    constructor(data: Any): this() {

        if (data is OutageFeature) {
            val geometry = data.geometry
            val attributes = data.attributes

            latitude            = geometry.y
            longitude           = geometry.x
            outageID            = attributes.NUM_1
            customersAffected   = attributes.NUM_CUST
            finishDate          = Date(attributes.EST_REP_TIME.toLong())
            startDate           = Date(attributes.OFF_DTS.toLong())
            updateDTS           = attributes.UPDATE_DTS
            status              = attributes.EVENT_STATUS_DESCRIPTION
            causeCode           = attributes.CAUSE_CODE
            supplemental1       = attributes.SUPPLEMENTAL_1
            siteID              = unknownString

        } else {
            throw Error("Cannot instantiate Active Outage")
        }

    }

    constructor() : super()

}