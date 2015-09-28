package com.weirded.iphone

import java.io.File

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import org.apache.commons.io.FileUtils
import spray.json._

import scala.util.control.NonFatal

trait StoreStore {
  def previousStoresWithInventory: Set[Store]
  def storePreviousStores(stores: Set[Store]): Unit
}

class FileStoreStore extends StoreStore with Protocols {

  private val file = new File("store/store.json")

  def storePreviousStores(stores: Set[Store]) = {
    FileUtils.writeStringToFile(file, new StoreListing("nope", stores.toSeq).toJson.prettyPrint)
  }

  def previousStoresWithInventory = {
    if (file.exists()) {
      FileUtils.readFileToString(file).parseJson.convertTo[StoreListing].stores.toSet
    }  else {
      Set.empty
    }
  }
}

class S3StoreStore extends StoreStore with Protocols {
  val s3 = new AmazonS3Client()
  def previousStoresWithInventory: Set[Store] = {
    val file = File.createTempFile("iphone", "json")
    try {
      s3.getObject(new GetObjectRequest("iphone-memory", "stores-with-inventory.json"), file)
      FileUtils.readFileToString(file).parseJson.convertTo[StoreListing].stores.toSet
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
        Set.empty
    }
  }

  def storePreviousStores(stores: Set[Store]): Unit = {
    val file = File.createTempFile("iphone", "json")
    FileUtils.writeStringToFile(file, new StoreListing("nope", stores.toSeq).toJson.prettyPrint)
    s3.putObject("iphone-memory", "stores-with-inventory.json", file)
    println("Updated S3 with file.")
  }
}