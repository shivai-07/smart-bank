package com.shashank.example.smartbank.report;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Reads test case rows from the inline-string XLSX workbook in test resources. */
final class TestDataWorkbookReader {
	private static final String RESOURCE = "/testdata/SmartBankTestData.xlsx";
	private static final String MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
	private static final String OFFICE_REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
	private static final String PACKAGE_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";

	private TestDataWorkbookReader() {
	}

	static Map<String, TestCaseDetails> read() {
		try (InputStream resource = TestDataWorkbookReader.class.getResourceAsStream(RESOURCE)) {
			if (resource == null) {
				throw new IllegalStateException("Test data workbook not found on classpath: " + RESOURCE);
			}
			Map<String, byte[]> entries = unzip(resource);
			String sheetPath = findFirstSheetPath(entries);
			return readRows(entries.get(sheetPath));
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to load test data workbook " + RESOURCE, exception);
		}
	}

	private static Map<String, byte[]> unzip(InputStream input) throws IOException {
		Map<String, byte[]> entries = new HashMap<>();
		try (ZipInputStream zip = new ZipInputStream(input)) {
			ZipEntry entry;
			while ((entry = zip.getNextEntry()) != null) {
				ByteArrayOutputStream content = new ByteArrayOutputStream();
				zip.transferTo(content);
				entries.put(entry.getName(), content.toByteArray());
			}
		}
		return entries;
	}

	private static String findFirstSheetPath(Map<String, byte[]> entries) throws Exception {
		Element workbook = parse(required(entries, "xl/workbook.xml"));
		NodeList sheets = workbook.getElementsByTagNameNS(MAIN_NS, "sheet");
		if (sheets.getLength() == 0) {
			throw new IllegalStateException("Workbook contains no worksheets");
		}
		String relationshipId = ((Element) sheets.item(0)).getAttributeNS(OFFICE_REL_NS, "id");
		Element relationships = parse(required(entries, "xl/_rels/workbook.xml.rels"));
		NodeList relationNodes = relationships.getElementsByTagNameNS(PACKAGE_REL_NS, "Relationship");
		for (int index = 0; index < relationNodes.getLength(); index++) {
			Element relation = (Element) relationNodes.item(index);
			if (relationshipId.equals(relation.getAttribute("Id"))) {
				String target = relation.getAttribute("Target");
				return target.startsWith("/") ? target.substring(1) : "xl/" + target;
			}
		}
		throw new IllegalStateException("First worksheet relationship was not found");
	}

	private static Map<String, TestCaseDetails> readRows(byte[] sheetBytes) throws Exception {
		Element worksheet = parse(sheetBytes);
		NodeList rowNodes = worksheet.getElementsByTagNameNS(MAIN_NS, "row");
		Map<String, TestCaseDetails> cases = new HashMap<>();
		for (int rowIndex = 1; rowIndex < rowNodes.getLength(); rowIndex++) {
			Element row = (Element) rowNodes.item(rowIndex);
			List<String> cells = new ArrayList<>();
			NodeList cellNodes = row.getElementsByTagNameNS(MAIN_NS, "c");
			for (int cellIndex = 0; cellIndex < cellNodes.getLength(); cellIndex++) {
				Element cell = (Element) cellNodes.item(cellIndex);
				int column = columnIndex(cell.getAttribute("r"));
				while (cells.size() <= column) {
					cells.add("");
				}
				NodeList textNodes = cell.getElementsByTagNameNS(MAIN_NS, "t");
				cells.set(column, textNodes.getLength() == 0 ? "" : textNodes.item(0).getTextContent());
			}
			if (cells.size() >= 4 && !cells.get(0).isBlank()) {
				cases.put(cells.get(0), new TestCaseDetails(cells.get(1), cells.get(2), cells.get(3)));
			}
		}
		return cases;
	}

	private static int columnIndex(String cellReference) {
		int index = 0;
		for (int position = 0; position < cellReference.length(); position++) {
			char character = cellReference.charAt(position);
			if (!Character.isLetter(character)) {
				break;
			}
			index = index * 26 + Character.toUpperCase(character) - 'A' + 1;
		}
		return index - 1;
	}

	private static Element parse(byte[] xml) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml)).getDocumentElement();
	}

	private static byte[] required(Map<String, byte[]> entries, String name) {
		byte[] bytes = entries.get(name);
		if (bytes == null) {
			throw new IllegalStateException("Workbook is missing " + name);
		}
		return bytes;
	}
}
