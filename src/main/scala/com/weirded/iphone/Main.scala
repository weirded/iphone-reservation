package com.weirded.iphone

import java.io.File

import com.amazonaws.util.StringInputStream
import org.apache.commons.io.FileUtils

object Main extends App with Protocols {
  require(args.length > 0, "Please provide a .json configuration file!")
  val instance = new iPhoneCheckHandler
  val requestJson = FileUtils.readFileToString(new File(args.head))
  while (true) {
    Thread.sleep(1000)
    instance.handleRequest(new StringInputStream(requestJson), null, null)
  }
}


