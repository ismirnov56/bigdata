import org.apache.log4j.{Level, Logger}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession
import java.time.LocalDateTime
import java.sql.Timestamp
import java.time.format.DateTimeFormatter

object Parquest {
  def main(args: Array[String]): Unit = {
    System.setProperty("hadoop.home.dir", "C:\\hadoop\\3.3.0\\");
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    Logger.getLogger("org.spark-project").setLevel(Level.WARN)

    val Seq(masterURL, dataPath) = args.toSeq
    val cfg = new SparkConf().setAppName("Test").setMaster(masterURL)
    val sc = new SparkContext(cfg)
    sc.setLogLevel("error")
    val spark = SparkSession
      .builder()
      .getOrCreate()
    // For implicit conversions like converting RDDs to DataFrames
    import spark.implicits._
    val data = sc.textFile(dataPath)

    val indexLast = data.count()-1
    val rows = data.zipWithIndex().filter(x => (x._2!=0) && (x._2!=1) && (x._2!= indexLast) )

    val xmlRows = rows.map(x =>  scala.xml.XML.loadString(x._1))

    val rowsInternal = xmlRows.mapPartitions(attrs =>{
      val timeFormat =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
      attrs.map(attr =>
        new Row(
          Id = try{attr.attributes("Id").text.toInt} catch {case ex: NullPointerException => 0},
          PostTypeId = try{attr.attributes("PostTypeId").text.toInt} catch {case ex: NullPointerException => 0},
          CreationDate = Timestamp.valueOf(try {LocalDateTime.parse(attr.attributes("CreationDate").text, timeFormat)} catch {case ex: NullPointerException => LocalDateTime.now()}),
          Score = try{attr.attributes("Score").text.toInt} catch {case ex: NullPointerException => 0},
          ViewCount =try{ attr.attributes("ViewCount").text.toInt} catch {case ex: NullPointerException => 0},
          Body = try{attr.attributes("Body").text} catch {case ex: NullPointerException => "null"},
          OwnerUserId = try{attr.attributes("OwnerUserId").text.toInt} catch {case ex: NullPointerException => 0},
          LastEditorUserId = try{attr.attributes("LastEditorUserId").text.toInt} catch {case ex: NullPointerException => 0},
          LastEditorDisplayName = try{attr.attributes("LastEditorDisplayName").text} catch {case ex: NullPointerException => "null"},
          LastEditDate = Timestamp.valueOf(try{LocalDateTime.parse( attr.attributes("LastEditDate").text, timeFormat)} catch {case ex: NullPointerException => LocalDateTime.now()}),
          LastActivityDate = Timestamp.valueOf(try{LocalDateTime.parse( attr.attributes("LastActivityDate").text, timeFormat)} catch {case ex: NullPointerException => LocalDateTime.now()}),
          Title =try{attr.attributes("Title").text} catch {case ex: NullPointerException => "null"},
          Tags = try{attr.attributes("Tags").text} catch {case ex: NullPointerException => "null"},
          AnswerCount = try{attr.attributes("AnswerCount").text.toInt} catch {case ex: NullPointerException => 0},
          CommentCount = try{attr.attributes("CommentCount").text.toInt} catch {case ex: NullPointerException => 0},
          FavoriteCount = try{attr.attributes("FavoriteCount").text.toInt} catch {case ex: NullPointerException => 0},
          CommunityOwnedDate = Timestamp.valueOf(try{LocalDateTime.parse( attr.attributes("CommunityOwnedDate").text, timeFormat)} catch {case ex: NullPointerException => LocalDateTime.now()})
        )
      )
    })

    val dataset = spark.createDataset(rowsInternal)
    dataset.show(100)
    dataset.write.parquet("dataset.parquet")
  }
}

case class Row(
                Id:Integer,
                PostTypeId: Integer,
                CreationDate: Timestamp,
                Score: Integer,
                ViewCount: Integer,
                Body: String,
                OwnerUserId: Integer,
                LastEditorUserId: Integer,
                LastEditorDisplayName: String,
                LastEditDate: Timestamp,
                LastActivityDate: Timestamp,
                Title: String,
                Tags: String,
                AnswerCount: Integer,
                CommentCount: Integer,
                FavoriteCount: Integer,
                CommunityOwnedDate: Timestamp
              )