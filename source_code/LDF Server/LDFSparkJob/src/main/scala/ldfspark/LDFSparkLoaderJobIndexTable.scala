import com.typesafe.config.{ConfigException, Config}
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.SaveMode
import org.apache.spark.SparkFiles
import org.apache.spark.rdd.RDD
import spark.jobserver.{SparkJobValid, SparkJobValidation, SparkSqlJob}


// The code to parse the n triples is based on code that is used in Sempala

case class TripleObjectIndexed
(
  subjectTripleObjectIndex: Int,
  subjectTripleObject: String,
  predicateTripleObjectIndex: Int,
  predicateTripleObject: String,
  objectTripleObjectIndex: Int,
  objectTripleObject: String
)

object LDFSparkLoaderJobIndexTable extends SparkSqlJob {

  override def runJob(sc: SQLContext, jobConfig: Config): Any = {
    import sc.implicits._

    // Input file name (default: "/data/input.nt")
    var dataSource = "/data/input.nt"
    try {
      dataSource = jobConfig.getString("data_source")
    } catch {
      case e: ConfigException.Missing => e.printStackTrace()
    }

    // Input file name (default: "/data/input.nt")
    var useRemote = false
    try {
      useRemote = jobConfig.getBoolean("use_remote")
    } catch {
      case e: ConfigException.Missing => e.printStackTrace()
      useRemote = false
    }

    if(useRemote) {        
        var tempFile = sc.sparkContext.addFile(dataSource)
        var fileName = SparkFiles.get(dataSource.split("/").last)
        var file = sc.sparkContext.textFile(fileName)
        return readFile(file, sc)
    } else {
        var file = sc.sparkContext.textFile(dataSource)
        return readFile(file, sc)
    }
  }

  def readFile(file: RDD[String], sc: SQLContext) = {
    import sc.implicits._
    val triples = file.map(p => _parseTriple(p, " ", true)).toDF() // Field del: " ", Expand = True
    triples.write.mode(SaveMode.Overwrite).parquet("triple_table_spark.parquet")
    
    val triplesParquetFile = sc.read.parquet("triple_table_spark.parquet")
    triplesParquetFile.registerTempTable("triple_table")
    sc.cacheTable("triple_table")

    // Return the total number of parsed triples
    triples.count()
  }

  override def validate(sc: SQLContext, config: Config): SparkJobValidation = SparkJobValid

  val prefixes: Array[Array[String]] =
  Array(
    Array( "foaf01:", "<http://xmlns.com/foaf/0.1/" ),
    Array( "foaf:", "<http://xmlns.com/foaf/" ),
    Array( "bench:", "<http://localhost/vocabulary/bench/" ),
    Array( "xsd:", "<http://www.w3.org/2001/XMLSchema#" ),
    Array( "dc:", "<http://purl.org/dc/elements/1.1/" ),
    Array( "dcterms:", "<http://purl.org/dc/terms/" ),
    Array( "dctype:", "<http://purl.org/dc/dcmitype/" ),
    Array( "rdf:", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#" ),
    Array( "rdfs:", "<http://www.w3.org/2000/01/rdf-schema#" ),
    Array( "swrc:", "<http://swrc.ontoware.org/ontology#" ),
    Array( "rss:", "<http://purl.org/rss/1.0/" ),
    Array( "owl:", "<http://www.w3.org/2002/07/owl#" ),
    Array( "person:", "<http://localhost/persons/" ),
    Array( "ex:", "<http://example.org/" ),
    Array( "node:", "<http://v.org/" ),
    Array( "sioc:", "<http://rdfs.org/sioc/ns#>" ),
    Array( "sioct:", "<http://rdfs.org/sioc/type#>" ),
    Array( "dbp:", "<http://dbpedia.org/resource/" ),
    Array( "dbpo:", "<http://dbpedia.org/ontology/" ),
    Array( "dbpprop:", "<http://dbpedia.org/property/" ),
    Array( "sib:", "<http://www.ins.cwi.nl/sib/vocabulary/>" ),
    Array( "sibp:", "<http://www.ins.cwi.nl/sib/person/>" ),
    Array( "sibu:", "<http://www.ins.cwi.nl/sib/user/>" ),
    Array( "sibfo:", "<http://www.ins.cwi.nl/sib/forum/>" ),
    Array( "sibfr:", "<http://www.ins.cwi.nl/sib/friendship/>" ),
    Array( "sibg:", "<http://www.ins.cwi.nl/sib/group/>" ),
    Array( "sibgm:", "<http://www.ins.cwi.nl/sib/group/membership/>" ),
    Array( "sibpo:", "<http://www.ins.cwi.nl/sib/post/>" ),
    Array( "sibc:", "<http://www.ins.cwi.nl/sib/post/comment/>" ),
    Array( "sibpho:", "<http://www.ins.cwi.nl/sib/photoalbum/photo/>" ),
    Array( "sibpha:", "<http://www.ins.cwi.nl/sib/photoalbum/>" ),
    Array( "lubm:","<http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#"),
    Array( "rdf:", "<http://www.w3.org/1999/02/22-rdf-syntax-ns#"  ),
    Array( "owl:", "<http://www.w3.org/2002/07/owl#" ),
    Array( "watdiv:", "<http://db.uwaterloo.ca/~galuc/wsdbm/" ),
    Array( "schema:", "<http://schema.org/" )
  )

   val prefixesArray: Array[String] =
   Array(
        "<http://xmlns.com/foaf/0.1/",
        "<http://xmlns.com/foaf/",
        "<http://localhost/vocabulary/bench/",
        "<http://www.w3.org/2001/XMLSchema#",
        "<http://purl.org/dc/elements/1.1/",
        "<http://purl.org/dc/terms/",
        "<http://purl.org/dc/dcmitype/",
        "<http://www.w3.org/1999/02/22-rdf-syntax-ns#",
        "<http://www.w3.org/2000/01/rdf-schema#",
        "<http://swrc.ontoware.org/ontology#",
        "<http://purl.org/rss/1.0/",
        "<http://www.w3.org/2002/07/owl#",
        "<http://localhost/persons/",
        "<http://example.org/",
        "<http://v.org/",
        "<http://rdfs.org/sioc/ns#>",
        "<http://rdfs.org/sioc/type#>",
        "<http://dbpedia.org/resource/",
        "<http://dbpedia.org/ontology/",
        "<http://dbpedia.org/property/",
        "<http://www.ins.cwi.nl/sib/vocabulary/>",
        "<http://www.ins.cwi.nl/sib/person/>",
        "<http://www.ins.cwi.nl/sib/user/>",
        "<http://www.ins.cwi.nl/sib/forum/>",
        "<http://www.ins.cwi.nl/sib/friendship/>",
        "<http://www.ins.cwi.nl/sib/group/>",
        "<http://www.ins.cwi.nl/sib/group/membership/>",
        "<http://www.ins.cwi.nl/sib/post/>",
        "<http://www.ins.cwi.nl/sib/post/comment/>",
        "<http://www.ins.cwi.nl/sib/photoalbum/photo/>",
        "<http://www.ins.cwi.nl/sib/photoalbum/>",
        "<http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#",
        "<http://www.w3.org/1999/02/22-rdf-syntax-ns#" ,
        "<http://www.w3.org/2002/07/owl#",
        "<http://db.uwaterloo.ca/~galuc/wsdbm/",
        "<http://schema.org/",
        "<http://purl.org/stuff/rev#",
        "<http://purl.org/ontology/mo/",
        "<http://purl.org/goodrelations/",
        "<http://www.geonames.org/ontology#",
        "<http://ogp.me/ns#"
  )

  def _parseTriple(inputString: String, fieldDel: String, expand: Boolean): TripleObjectIndexed = {

    var triple: String = inputString.trim().substring(0, inputString.length() - 1).trim()
    val tripleSplitted: Array[String] = splitTriple(triple, fieldDel)

    val s = parseSubject(tripleSplitted(0), expand)
    val p = parsePredicate(tripleSplitted(1), expand)
    val o = parseObject(tripleSplitted(2), expand)

    val sTuple = getIndex(s)
    val pTuple = getIndex(p)
    val oTuple = getIndex(o)

    TripleObjectIndexed(
      sTuple._1,
      sTuple._2,
      pTuple._1,
      pTuple._2,
      oTuple._1,
      oTuple._2
    )
  }

  def getIndex(triple: String): (Int, String) = {
    if(isURI(triple)) {
        for (i <- prefixesArray.indices) {
            if(triple.startsWith(prefixesArray(i))) {
                return ((i+1), triple.substring(prefixesArray(i).length,triple.length-1))
            }
        }

        return (0, triple)
    } else {
        return (0, triple)
    }
  }

  

  def splitTriple(triple: String, fieldDel: String): Array[String] = {
    val fields: Array[String] = new Array[String](3)

    // Subject
    var delPos = triple.indexOf(fieldDel)
    if(delPos == -1) return null

    fields(0) = triple.substring(0, delPos)
    if(fields(0) == null) return null

    var _triple = triple.substring(delPos + 1)
    while(_triple.startsWith(fieldDel)) {
      _triple = _triple.substring(1)
    }

    // Predicate
    delPos = _triple.indexOf(fieldDel)
    if(delPos == -1) return null

    fields(1) = _triple.substring(0, delPos)
    if(fields(1) == null) return null

    _triple = _triple.substring(delPos + 1)
    while(_triple.startsWith(fieldDel)) {
      _triple = _triple.substring(1)
    }

    // Object
    fields(2) = _triple
    if(fields(2) == null) return null

    return fields
  }

  def parseSubject(input: String, expand: Boolean): String = {
    if(isURI(input) || isBlankNode(input)) {
      return expandPrefix(input, expand)
    } else {
      return "" //null
    }
  }

  def parsePredicate(input: String, expand: Boolean): String = {
    if(isURI(input)) {
      return expandPrefix(input, expand)
    } else if (input.equals("a")) {
      return input
    } else {
      return "" //null
    }
  }

  def parseObject(input: String, expand: Boolean): String = {
    if(isURI(input) || isBlankNode(input)) {
      return expandPrefix(input, expand)
    } else {
      return parseLiteralObject(input, expand)
    }
  }

  def parseLiteralObject(input: String, expand: Boolean): String = {
    if (input.matches("[0-9]+") // Int
      || input.matches("[0-9]+\\.[0-9]+") // Float
      || input.matches("\"(.)*\"") // String
      || (input.charAt(0) == '"' && input.charAt(input.length() - 1) == '"')) {
      val inputParsed = input.replaceAll("\"", "")
      return inputParsed
    }
    // if it is not a number nor a plain literal it has to be a typed
    // literal
    else
      return parseTypedLiteralObject(input, expand)
  }

  def parseTypedLiteralObject(input: String, expand: Boolean): String = {
    // typed literals have to start with quotation mark (")
    if (!input.startsWith("\"")) {
      return null; // syntax error
    }
    // extract the literal out of the typed literal
    val endOfLiteral = input.lastIndexOf("\"") + 1
    val literal = input.substring(0, endOfLiteral)
    // empty literals ("") are not allowed
    if (literal.length() <= 1) {
      return null; // syntax error
    }
    // check if it has a language tag
    else if (input.substring(endOfLiteral, endOfLiteral + 1).equals("@")) {
      /*
       * extract the language tag out of the typed literal, normalize the
       * language tag and return the normalized typed literal
       */
      var languageTag = input.substring(endOfLiteral + 1, input.length())
      languageTag = languageTag.toLowerCase()
      return literal + "@" + languageTag
    }
    // check if it has a datatype
    else if (input.substring(endOfLiteral, endOfLiteral + 2).equals("^^")) {
      /*
       * extract the datatype out of the typed literal, expand prefixes if
       * wanted and return the typed literal
       */
      val datatype = input.substring(endOfLiteral + 2, input.length())
      return literal.substring(1, literal.length() - 1) + "^^" + expandPrefix(datatype, expand)
    }
    // if the typed literal has neither a language tag nor a datatype it is
    // a syntax error
    return null
  }

  def isURI(input: String): Boolean = {
    if (input.startsWith("<") && input.endsWith(">")) {
      return true
    } else if (startsWithPrefix(input)) {
      return true
    } else {
      return false
    }
  }

  def isBlankNode(input: String): Boolean = {
    if(input.startsWith("_:")) {
      return true
    } else {
      return false
    }
  }

  def startsWithPrefix(input: String): Boolean = {
    for (i <- prefixes.indices) {
      if(input.startsWith(prefixes(i)(0)))
        return true
    }
    return false
  }

  def expandPrefix(input: String, expand: Boolean): String = {
    if(!expand) return input
    if(input.startsWith("<") && input.endsWith(">")) return input
    if(input.startsWith("_:")) return input

    var _input = input
    for(i <- prefixes.indices) {
      if(input.startsWith(prefixes(i)(0))) {
        _input = _input.replace(prefixes(i)(0), prefixes(i)(1))
        return _input + ">"
      }
    }

    return null
  }
}
