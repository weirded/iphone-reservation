package com.weirded.iphone

import java.io.File

import org.apache.commons.io.FileUtils
import spray.json._

import scala.util.control.NonFatal

object Main extends App with Protocols {
  require(args.length > 0, "Please provide a .json configuration file!")
  while (true) {
    Thread.sleep(30 * 1000)
    try {
      val requestJson = FileUtils.readFileToString(new File(args.head))
      val instance = new iPhoneChecker(requestJson.parseJson.convertTo[Request], new FileStoreStore)
      instance.check()
    } catch {
      case NonFatal(e) =>
        e.printStackTrace()
    }
  }
}


