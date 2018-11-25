package es.weso.schemaInfer
import es.weso.rdf.nodes.IRI

/**
*
  * @param check takes a label and an property IRI and return Left is the followOn is not satisfied,
  *              or Right(genIri) if the followOn condition is satisfied with a generated shape label genIri
  */
case class FollowOn(name: String,
                    check: (IRI,IRI,Int) => Either[String,IRI]
                   )

object FollowOn {

  val followOnReference: FollowOn =
    FollowOn("WikidataReference", { case (label, prop, _) => {
      val wdPropRegex = s"^${IRI("http://www.wikidata.org/prop").str}(P\\d*)".r
      prop.str match {
        case wdPropRegex(prop) => Right(label.resolve(IRI(prop + "Prop")))
        case _ => Left(s"$prop does not match $wdPropRegex")
      }
    }}
   )

  val followOnWasDerivedFrom: FollowOn =
    FollowOn("wikidataWasDerivedFrom", {
    case (label,prop, _) => {
      val prov = IRI("http://www.w3.org/ns/prov#")
      val `prov:wasDerivedFrom` = prov + "wasDerivedFrom"
      prop match {
        case `prov:wasDerivedFrom` => Right(label + "Ref")
        case _ => Left(s"$prop does not match ${`prov:wasDerivedFrom`}")
      }
    }})

  def followOnStem(stem: IRI): FollowOn =
    FollowOn(s"followOnStem($stem)", { case (label, prop, num) => {
      // println(s"Checking FollowOn($stem) for $prop with label: $label")
      val stemRegex = s"^${stem.str}(.*)".r
      prop.str match {
        case stemRegex(rest) => {
          val suffix = if (num == 1) "" else num.toString
          println(s"Matches with $rest, num: $num, suffix: $suffix")
          Right(label.resolve(IRI(rest + ("Shape" + suffix))))
        }
        case _ => Left(s"$prop does not match $stemRegex")
      }
    }}
    )

  /*  val followOnGeneric = {
    def checkGeneric(visited: List[IRI])(label:IRI, prop: IRI): Either[String, IRI] =
      if (visited contains prop) Left("Property has already been inferred")
      else Right(prop + "Shape")

    FollowOn("generic", checkGeneric)
  } */
}