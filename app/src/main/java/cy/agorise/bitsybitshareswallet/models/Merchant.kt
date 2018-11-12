package cy.agorise.bitsybitshareswallet.models

import java.util.*

class Merchant {

    var id: String? = null
    var address: String? = null
    var phone: String? = null
    var name: String? = null
    var lat: Float = 0.toFloat()
    var lon: Float = 0.toFloat()
    var city: String? = null
    var country: String? = null
    var createdAt: Date? = null
    var updatedAt: Date? = null
    var __v: Int = 0

    constructor() {}
    constructor(id: String) {
        this.id = id
    }

    constructor(_id: String, address: String, phone: String, name: String, lat: Float, lon: Float, city: String, country: String, createdAt: Date, updatedAt: Date, __v: Int) {
        this.id = _id
        this.address = address
        this.phone = phone
        this.name = name
        this.lat = lat
        this.lon = lon
        this.city = city
        this.country = country
        this.createdAt = createdAt
        this.updatedAt = updatedAt
        this.__v = __v
    }
}