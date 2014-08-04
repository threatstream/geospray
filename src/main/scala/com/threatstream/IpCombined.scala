package com.threatstream

import com.snowplowanalytics.maxmind.iplookups.IpLookupResult


/**
 * Simple class to transform an IpLookupResult into a combined geo + org + isp + netspeed etc.. object.
 */
case class IpCombined(
    val countryCode: Option[String] = None,
    val countryName: Option[String] = None,
    val region: Option[String] = None,
    val city: Option[String] = None,
    val latitude: Option[Float] = None,
    val longitude: Option[Float] = None,
    val postalCode: Option[String] = None,
    val dmaCode: Option[Int] = None,
    val areaCode: Option[Int] = None,
    val metroCode: Option[Int] = None,
    val regionName: Option[String] = None,
    val isp: Option[String] = None,
    val org: Option[String] = None,
    val domain: Option[String] = None,
    val netspeed: Option[String] = None,
    val asn: Option[Int] = None
)

object IpCombined {
    private val _asnParserPattern = """^AS([0-9]+) (.*)""".r

    def apply(ipLookupResult: IpLookupResult, asnInfo: String): IpCombined = {
        val (asn, isp) = asnInfo match {
            case _asnParserPattern(asn, isp) => (Some(asn.toInt), Some(isp))
            case _ => (None, None)
        }
        ipLookupResult._1 match {
            case Some(geo) =>
                IpCombined(
                    Some(geo.countryCode),
                    Some(geo.countryName),
                    geo.region,
                    geo.city,
                    Some(geo.latitude),
                    Some(geo.longitude),
                    geo.postalCode,
                    geo.dmaCode,
                    geo.areaCode,
                    geo.metroCode,
                    geo.regionName,
                    if (ipLookupResult._2 != None) { ipLookupResult._2 } else { isp }, // isp
                    ipLookupResult._3, // org
                    ipLookupResult._4, // domain
                    ipLookupResult._5, // netspeed
                    asn // asn
                )

            case None => IpCombined(isp = ipLookupResult._2, org = ipLookupResult._3, domain = ipLookupResult._4, netspeed = ipLookupResult._5)
        }
    }

    implicit def ipLookupResultToCombined(ipLookupResultAndAsn: Tuple2[IpLookupResult, String]): IpCombined = apply(ipLookupResultAndAsn._1, ipLookupResultAndAsn._2)
}
