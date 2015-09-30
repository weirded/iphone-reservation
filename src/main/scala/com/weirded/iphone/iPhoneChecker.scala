package com.weirded.iphone

import com.twilio.sdk.TwilioRestClient
import org.apache.commons.io.IOUtils
import org.apache.commons.logging.LogFactory
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.message.BasicNameValuePair
import org.apache.http.util.EntityUtils
import spray.json.{AdditionalFormats, JsObject, _}

import scala.collection.JavaConversions._

case class iPhoneModel(modelNumber: String, model: String, capacityGb: Int, color: String, carrier: String)

case class Store(storeNumber: String, storeName: String, storeEnabled: Boolean, storeState: String, sellEdition: Boolean, storeCity: String)

case class StoreListing(updatedTime: String, stores: Seq[Store])

case class TwilioConfiguration(accountSid: String,
                               authToken: String,
                               fromPhone: String)

case class SearchParameters(desiredStates: Seq[String],
                            desiredCities: Seq[String],
                            desiredCapacities: Seq[Int],
                            desiredModelNames: Seq[String],
                            desiredColors: Seq[String],
                            desiredCarriers: Seq[String])

case class Request(searchParameters: SearchParameters, toPhone: String, twilioConfiguration: TwilioConfiguration)

trait Protocols extends DefaultJsonProtocol {
  implicit val twilioConfigurationFormat = jsonFormat3(TwilioConfiguration)
  implicit val searchParametersFormat = jsonFormat6(SearchParameters)
  implicit val requestFormat = jsonFormat3(Request)
  implicit val storeFormat = jsonFormat6(Store)
  implicit val storeListingFormat = jsonFormat2(StoreListing)
}

class iPhoneChecker(request: Request, storeStore: StoreStore) extends Protocols with AdditionalFormats {

  val stores = retrieveStoreListing.stores

  val log = LogFactory.getLog(getClass)

  request.searchParameters.desiredStates.foreach {
    state =>
      require(stores.exists(_.storeState == state), s"No stores in state $state")
  }

  request.searchParameters.desiredCities.foreach {
    city =>
      require(stores.exists(_.storeCity == city), s"No stores in city $city")
  }

  val models = loadModelData

  request.searchParameters.desiredCapacities.foreach {
    capacity =>
      require(models.exists(_.capacityGb == capacity), s"No model with $capacity GB storage")
  }

  request.searchParameters.desiredModelNames.foreach {
    model =>
      require(models.exists(_.model == model), s"No model named $model")
  }

  request.searchParameters.desiredColors.foreach {
    color =>
      require(models.exists(_.color == color), s"No color named $color")
  }

  request.searchParameters.desiredCarriers.foreach {
    carrier =>
      require(models.exists(_.carrier == carrier), s"No carrier named $carrier")
  }

  val desiredModels = models.filter {
    model =>
      request.searchParameters.desiredModelNames.contains(model.model) &&
          request.searchParameters.desiredCapacities.contains(model.capacityGb) &&
          request.searchParameters.desiredCarriers.contains(model.carrier) &&
          request.searchParameters.desiredColors.contains(model.color)
  }

  val desiredStores = stores.filter {
    store =>
      request.searchParameters.desiredStates.contains(store.storeState) &&
          request.searchParameters.desiredCities.contains(store.storeCity)
  }

  def check(): Unit = {
    val storesWithInventory = storesWithPhones
    val newStores = storesWithInventory.diff(storeStore.previousStoresWithInventory)
    storeStore.storePreviousStores(storesWithInventory)
    if (newStores.nonEmpty) {
      val storeList = newStores.map(_.storeName).mkString(", ")
      sendSMS(s"iPhones now available in $storeList")
    }
  }

  private def storesWithPhones: Set[Store] = {

    val availability = retrieveAvailability
    def available(storeNumber: String, model: String): Boolean = {
      availability.fields.find(_._1 == storeNumber).map {
        storeInventory =>
          storeInventory._2.asJsObject.fields.find(_._1 == model).map {
            inventoryItem =>
              val itemAvailable = inventoryItem._2.toString()
              require(itemAvailable != "ALL" && itemAvailable != "NONE",
                s"Unknown availability value: $itemAvailable")
              itemAvailable.contains("ALL")
          }.getOrElse(throw new IllegalArgumentException("Unknown item!"))
      }.getOrElse(throw new IllegalArgumentException("Unknown store!"))
    }

    log.info(s"Checking ${desiredStores.size} stores for ${desiredModels.size} models ...")
    val storesWithInventory = desiredStores.map {
      store =>
        val availableModels = desiredModels.filter(m => available(store.storeNumber, m.modelNumber))
        store -> availableModels.toSet
    }.filter(_._2.nonEmpty)

    storesWithInventory.toSeq.sortBy(s => s._1.storeCity + s._1.storeName).foreach {
      case (store, models) =>
        log.info(s"${store.storeState} - ${store.storeCity} - ${store.storeName} ")
        models.toSeq.sortBy(m => m.capacityGb + m.color).foreach {
          model =>
            log.info(s"  ${model.model} ${model.capacityGb} GB color: ${model.color} carriers: ${model.carrier}")
        }
    }

    storesWithInventory.map(_._1).toSet
  }

  private def retrieveStoreListing: StoreListing = {
    val client = new DefaultHttpClient()
    val response = client.execute(new HttpGet("https://reserve.cdn-apple.com/US/en_US/reserve/iPhone/stores.json"))
    val entity = EntityUtils.toString(response.getEntity)
    entity.parseJson.convertTo[StoreListing]
  }

  private def retrieveAvailability: JsObject = {
    val client = new DefaultHttpClient()
    val response = client.execute(new HttpGet("https://reserve.cdn-apple.com/US/en_US/reserve/iPhone/availability.json"))
    val entity = EntityUtils.toString(response.getEntity)
    entity.parseJson.asJsObject
  }

  private def loadModelData: Seq[iPhoneModel] = {
    val regex = """.*aria-label=\"(.*) (\d+)GB (.*), \$.* value="(.*)" type.*""".r
    val lines = IOUtils.readLines(getClass.getClassLoader.getResourceAsStream("model_data.txt"), "UTF-8").
        map(_.toString.trim)
    lines.flatMap {
      case regex(model, gigs, carrierAndColor, modelNumber) =>
        val lastSpace = carrierAndColor.lastIndexOf(" ")
        val carrier = carrierAndColor.substring(lastSpace).replace("&amp;", "&").trim
        val color = carrierAndColor.substring(0, lastSpace).trim
        Some(iPhoneModel(modelNumber, model, gigs.toInt, color, carrier))
      case _ =>
        None
    }
  }

  private def sendSMS(text: String): Unit = {
    val twilio = new TwilioRestClient(request.twilioConfiguration.accountSid, request.twilioConfiguration.authToken)
    val params = List(
      new BasicNameValuePair("Body", text),
      new BasicNameValuePair("To", request.toPhone),
      new BasicNameValuePair("From", request.twilioConfiguration.fromPhone)
    )
    val messageFactory = twilio.getAccount.getMessageFactory
    val message = messageFactory.create(params)
    log.info(s"Sent SMS: ${message.getSid}")
  }
}
