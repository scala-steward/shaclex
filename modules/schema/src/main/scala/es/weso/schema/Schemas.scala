package es.weso.schema
import java.io.File

// import util._
import cats.effect._
// import cats.implicits._
import es.weso.rdf.RDFReader
// import es.weso.utils.FileUtils._
import java.nio.file.Paths
// import es.weso.utils.IOUtils
import es.weso.utils.FileUtils

object Schemas {

  type SchemaParser = (String, String, Option[String]) => IO[Schema]

  lazy val shEx: Schema = ShExSchema.empty
  lazy val shaclex : Schema = ShaclexSchema.empty
  lazy val jenaShacl : Schema = JenaShacl.empty
  lazy val shaclTQ = ShaclTQ.empty

  val availableSchemas: List[Schema] = List(shEx, shaclex, jenaShacl, shaclTQ) 
  val defaultSchema: Schema = shEx
  val defaultSchemaName: String = defaultSchema.name
  val defaultSchemaFormat: String = defaultSchema.defaultFormat

  val availableSchemaNames: List[String] = availableSchemas.map(_.name)

  val availableFormats: List[String] = {
    availableSchemas.map(_.formats).flatten.distinct
  }


  val availableTriggerModes: List[String] = {
    ValidationTrigger.triggerValues.map(_._1)
  }

  val defaultTriggerMode: String =
    ValidationTrigger.default.name

  def lookupSchema(schemaName: String): IO[Schema] = {
    if (schemaName == "") IO.pure(defaultSchema)
    else {
      val found = availableSchemas.filter(_.name.compareToIgnoreCase(schemaName) == 0)
      if (found.isEmpty)
        IO.raiseError(new RuntimeException(s"Schema $schemaName not found. Available schemas: ${availableSchemaNames.mkString(",")}"))
      else
        IO.pure(found.head)
    }
  }

  def getSchemaParser(schemaName: String): IO[SchemaParser] = for {
    schema <- lookupSchema(schemaName)
  } yield schema.fromString _

  val schemaNames: List[String] = availableSchemas.map(_.name)

  def fromFile(
    file: File,
    format: String,
    schemaName: String,
    base: Option[String] = None): IO[Schema] =
    for {
     cs <- FileUtils.getContents(Paths.get(file.getAbsolutePath()))
     schema <- fromString(cs, format, schemaName, base)
    } yield schema

  def fromString(
    str: String,
    format: String,
    schemaName: String,
    base: Option[String] = None): IO[Schema] = for {
    schema <- lookupSchema(schemaName)
    schemaParsed <- if (str.length == 0) IO.pure(schema.empty)
                    else schema.empty.fromString(str, format, base)
  } yield schemaParsed

/*  private def getFileContents(file: File): IO[CharSequence] = {
    getContents(Paths.get(file.getAbsolutePath())).value.flatMap(e => e match {
      case Left(s) => IO.raiseError(new RuntimeException(s))
      case Right(cs) => IO.pure(cs)
    })
  } */

  def fromRDF(rdf: RDFReader, schemaName: String): IO[Schema] = {
    for {
      defaultSchema <- lookupSchema(schemaName)
      schema <- defaultSchema.fromRDF(rdf)
    } yield schema
  }

  def fromRDFIO(rdf: RDFReader, schemaName: String): IO[Schema] = for {
    defaultSchema <- lookupSchema(schemaName)
    maybeRDF <- defaultSchema.fromRDF(rdf)
  } yield maybeRDF
/*    lookupSchema(schemaName) match {
      case Left(s) => IO.raiseError(new RuntimeException(s"fromRDFIO: Cannot obtain default schema ${schemaName}: $s"))
      case Right(defaultSchema) => defaultSchema.fromRDF(rdf).value.flatMap(either => either.fold(
        s => IO.raiseError(new RuntimeException(s"fromRDFIO: Error obtaining schema ${schemaName}: $s")),
        schema => IO(schema)
      ))
    } */

}
