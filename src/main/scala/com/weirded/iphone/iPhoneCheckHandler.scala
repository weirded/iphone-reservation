package com.weirded.iphone

import java.io.{InputStream, OutputStream}

import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import org.apache.commons.io.IOUtils
import spray.json._

class iPhoneCheckHandler extends RequestStreamHandler with Protocols {
  override def handleRequest(inputStream: InputStream, outputStream: OutputStream, context: Context) = {
    val request = IOUtils.toString(inputStream).parseJson.convertTo[Request]
    val checker = new iPhoneChecker(request, new S3StoreStore)
    checker.check()
  }
}
