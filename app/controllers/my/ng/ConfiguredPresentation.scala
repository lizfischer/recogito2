package controllers.my.ng

import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._
import services.{HasDate, Page}
import services.generated.tables.records.{DocumentRecord, DocumentFilepartRecord, SharingPolicyRecord}

case class IndexDerivedProperties(
  lastEditAt: Option[DateTime],
  lastEditBy: Option[String],
  annotations: Option[Long]
)

object IndexDerivedProperties {

  // Shorthand
  val EMPTY = IndexDerivedProperties(None, None, None)

}

case class ConfiguredPresentation(
  document: DocumentRecord, 
  parts: Seq[DocumentFilepartRecord],
  sharedVia: Option[SharingPolicyRecord],
  indexProps: IndexDerivedProperties,
  columnConfig: Seq[String]
) {

  // Helper to get the value of a DB property, under consideration of the columConfig
  def getDBProp[T](key: String, prop: T): Option[T] = 
    if (columnConfig.contains(key)) Option(prop) else None

  // The only difference for index props it that they are already properly Option-typed
  def getIndexProp[T](key: String, prop: Option[T]): Option[T] =
    if (columnConfig.contains(key)) prop else None

}

object ConfiguredPresentation extends HasDate {

  val DEFAULT_CONFIG = Seq("author", "title", "uploaded_at", "date_freeform")

  /** A helper to get the properties for the given doc from the list **/
  private def findProps(document: DocumentRecord, indexProps: Option[Map[String, IndexDerivedProperties]]) =
    indexProps.flatMap { p =>
      p.get(document.getId)
    }.getOrElse(IndexDerivedProperties.EMPTY)

  /** Builds a configured presentation.
    * 
    * Takes a DB search result, index-derived props and the column config 
    * as input. Column config is optional and will default to a sensible value.
    */
  def forMyDocument(
    page: Page[(DocumentRecord, Seq[DocumentFilepartRecord])],
    indexProps: Option[Map[String, IndexDerivedProperties]],
    columnConfig: Option[Seq[String]]
  ) = {
    val config = columnConfig.getOrElse(DEFAULT_CONFIG)
    page.map { case (document, parts) =>
      val props = findProps(document, indexProps)
      ConfiguredPresentation(document, parts, None, props, config)
    }
  }

  def forSharedDocument(
    page: Page[(DocumentRecord, SharingPolicyRecord, Seq[DocumentFilepartRecord])],
    indexProps: Option[Map[String, IndexDerivedProperties]],
    columnConfig: Option[Seq[String]]
  ) = {
    val config = columnConfig.getOrElse(DEFAULT_CONFIG)
    page.map { case (document, sharingPolicy, parts) => 
      val props = findProps(document, indexProps)
      ConfiguredPresentation(document, parts, Some(sharingPolicy), props, config)
    }
  }

  implicit val configuredPresentationWrites: Writes[ConfiguredPresentation] = (
    // Write mandatory properties in any case
    (JsPath \ "id").write[String] and
    (JsPath \ "owner").write[String] and 
    (JsPath \ "uploaded_at").write[DateTime] and
    (JsPath \ "title").write[String] and

    (JsPath \ "filetypes").write[Seq[String]] and
    (JsPath \ "file_count").write[Int] and

    // Selectable DB properties
    (JsPath \ "author").writeNullable[String] and 
    (JsPath \ "date_freeform").writeNullable[String] and
    // TODO description?
    (JsPath \ "language").writeNullable[String] and
    (JsPath \ "source").writeNullable[String] and
    (JsPath \ "edition").writeNullable[String] and
    (JsPath \ "public_visibility").writeNullable[String] and

    // Selectable index properties
    (JsPath \ "last_edit_at").writeNullable[DateTime] and
    (JsPath \ "last_edit_by").writeNullable[String] and
    (JsPath \ "annotations").writeNullable[Long]
  )(p => (
    p.document.getId,
    p.document.getOwner,
    new DateTime(p.document.getUploadedAt.getTime),
    p.document.getTitle,

    p.parts.map(_.getContentType).distinct,
    p.parts.size,

    // DB-based props, based on whether they are defined in the column config
    p.getDBProp[String]("author", p.document.getAuthor),
    p.getDBProp[String]("date_freeform", p.document.getDateFreeform),
    p.getDBProp[String]("language", p.document.getLanguage),
    p.getDBProp[String]("source", p.document.getSource),
    p.getDBProp[String]("edition", p.document.getEdition),
    p.getDBProp[String]("public_visibility", p.document.getPublicVisibility),

    // Index-based properties
    p.getIndexProp[DateTime]("last_edit_at", p.indexProps.lastEditAt),
    p.getIndexProp[String]("last_edit_by", p.indexProps.lastEditBy),
    p.getIndexProp[Long]("annotations", p.indexProps.annotations)
  ))

}
