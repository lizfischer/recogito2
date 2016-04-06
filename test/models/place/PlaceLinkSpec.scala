package models.place

import java.util.UUID
import org.specs2.mutable._
import org.specs2.runner._
import org.junit.runner._
import play.api.libs.json.Json
import play.api.test._
import play.api.test.Helpers._
import scala.io.Source

@RunWith(classOf[JUnitRunner])
class PlaceLinkSpec extends Specification {

  "The sample place link" should {
    
    "be properly created from JSON" in {
      val json = Source.fromFile("test/resources/place-link.json").getLines().mkString("\n")
      val result = Json.fromJson[PlaceLink](Json.parse(json))
      
      // Parsed without errors?
      result.isSuccess must equalTo(true) 
      
      val placeLink = result.get
      placeLink.placeId must equalTo("http://dare.ht.lu.se/places/10778")
      placeLink.annotationId must equalTo(UUID.fromString("5c25d207-11a5-49f0-b2a7-61a6ae63d96c"))
      placeLink.documentId must equalTo("qhljvnxnuuc9i0")
      placeLink.filepartId must equalTo(19)
      placeLink.gazetteerUri must equalTo("http://pleiades.stoa.org/places/118543")
    }
    
  }
  
  "JSON serialization/parsing roundtrip" should {
    
    "yield an equal place link" in {
      val placeLink = PlaceLink(
        "http://dare.ht.lu.se/places/10778",
        UUID.randomUUID(),
        "qhljvnxnuuc9i0",
        19,
        "http://pleiades.stoa.org/places/118543")
        
      // Convert to JSON
      val serialized = Json.prettyPrint(Json.toJson(placeLink))
      
      val parseResult = Json.fromJson[PlaceLink](Json.parse(serialized))
      parseResult.isSuccess must equalTo(true)
      parseResult.get must equalTo(placeLink)
    }
    
  }
  
}