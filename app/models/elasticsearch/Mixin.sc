
// Retrieving large documents via ES is slow
/* override def getDocumentById(docId: Long)(index: String): Option[Document] = {
  val fields = getDocument(docId, List(utils.docContentField, utils.docDateField))(index)
  // TODO Add apply to Document?
  if (fields.isEmpty) None else Some(Document(docId, fields(utils.docContentField), LocalDateTime.parse(fields(utils.docDateField), utils.yearMonthDayFormat)))
}

private def getDocument(docId: Long, fields: List[String])(index: String): Map[String, String] = {
  val response = clientService.client.prepareGet(index, null, docId.toString).setFields(fields: _*).execute().actionGet()
  fields.map(f => f -> utils.getFieldValueString(response, f).getOrElse("")).toMap
} */
