# Redacting Word and Excel Documents

`filterWord(context, policyName, filename, file)` sends a Word document (`.docx`) to Philter and returns the redacted document, and `filterExcel(...)` does the same for an Excel workbook (`.xlsx`). The result is an editable document of the same format, with identified information replaced according to the policy's filter strategies, as for text. Requires Philter 4.1.0 or later.

```java
import ai.philterd.philter.model.BinaryFilterResponse;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

BinaryFilterResponse response =
        client.filterWord("my-context", "default", "letter.docx", new File("letter.docx"));

Files.write(Path.of("letter-redacted.docx"), response.getContent());
String documentId = response.getDocumentId();
```

```java
BinaryFilterResponse response =
        client.filterExcel("my-context", "default", "claims.xlsx", new File("claims.xlsx"));

Files.write(Path.of("claims-redacted.xlsx"), response.getContent());
```

Each method also takes the document as a `byte[]`, so an upload can be redacted without first writing an unredacted copy to disk. There is no ZIP variant, and no PDF output.

## Submitting a document asynchronously

`filterWordAsync(...)` and `filterExcelAsync(...)` submit the document and return the ID Philter assigned to it. Poll the status and download the result as for a [PDF](pdf.md#submitting-a-document-asynchronously); `getDocument(documentId)` returns the redacted document's bytes in the same format.

```java
String documentId = client.filterWordAsync("my-context", "default", "letter.docx", new File("letter.docx"));

DocumentStatus status = client.getDocumentState(documentId);

byte[] docx = client.getDocument(documentId);           // once the status is COMPLETE
```

Pass a [fairness key](pdf.md#sharing-the-queue-fairly) as the last argument to share the queue among the people one API key submits for. The synchronous methods do not take one.

## What is redacted

Philter redacts the copies of text Office documents keep outside the visible body as well, such as headers and footers, comments, text boxes, chart text, cached values, and embedded Word and Excel documents. Embedded objects it cannot inspect are removed.

Known limitations:

* Document properties, such as the author, are not redacted, and neither are the author names Word records on comments and tracked changes.
* A redacted Word paragraph loses its inline formatting, and hyperlinks and fields in it become plain text.
* A redacted Excel cell is written as text, and a redacted formula cell loses its formula.
* Excel cells are scanned by their stored value, not as Excel displays them. A date displayed as `3/14/1985` is stored as the serial number `31120` and is not found by the date filter.

Review the redacted output against your own documents.

## Errors

Philter checks the body against the declared type before redacting it. These surface as a `ClientException` carrying Philter's message (see [Error Handling](error-handling.md)):

* **415** for a body that is not a document of the declared type: an Excel workbook sent to `filterWord`, any other ZIP file, a legacy `.doc` or `.xls` file, a password-protected document, or a document saved as Strict Open XML. Only the Transitional Office Open XML format, which Word and Excel save by default, is supported.
* **413** for a document over Philter's Office limits on decompressed size or number of entries, or with an entry larger than 100 KB that expands to more than 100 times its compressed size.
* **400** for a ZIP that cannot be read as a document.
