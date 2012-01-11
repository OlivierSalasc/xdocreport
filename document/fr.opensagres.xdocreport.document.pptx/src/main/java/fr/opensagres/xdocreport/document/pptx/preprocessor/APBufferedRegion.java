package fr.opensagres.xdocreport.document.pptx.preprocessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.xml.sax.Attributes;

import fr.opensagres.xdocreport.core.utils.StringUtils;
import fr.opensagres.xdocreport.document.preprocessor.sax.BufferedElement;
import fr.opensagres.xdocreport.document.preprocessor.sax.ISavable;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;
import fr.opensagres.xdocreport.template.formatter.IDocumentFormatter;

public class APBufferedRegion extends BufferedElement {

	private final PPTXSlideDocument document;
	private final List<ARBufferedRegion> arBufferedRegions;
	private Integer level;
	private String itemNameList;

	private List<String> ignoreLoopDirective;

	public APBufferedRegion(PPTXSlideDocument document, BufferedElement parent,
			String uri, String localName, String name, Attributes attributes) {
		super(parent, uri, localName, name, attributes);
		this.document = document;
		this.arBufferedRegions = new ArrayList<ARBufferedRegion>();
		this.ignoreLoopDirective = null;
	}

	@Override
	public void addRegion(ISavable region) {
		if (region instanceof ARBufferedRegion) {
			arBufferedRegions.add((ARBufferedRegion) region);
		} else {
			super.addRegion(region);
		}
	}

	public void process() {
		Collection<BufferedElement> toRemove = new ArrayList<BufferedElement>();
		int size = arBufferedRegions.size();
		String s = null;
		StringBuilder fullContent = new StringBuilder();
		ARBufferedRegion currentAR = null;
		ARBufferedRegion lastAR = null;
		boolean hasField = false;
		boolean fieldParsing = false;
		for (int i = 0; i < size; i++) {
			currentAR = arBufferedRegions.get(i);
			s = currentAR.getTContent();
			hasField = s != null && s.indexOf("$") != -1;
			if (fieldParsing) {
				// Field is parsing
				fieldParsing = (s == null || s.length() == 0 || Character
						.isWhitespace(s.charAt(0)));
				if (!fieldParsing) {
					if (hasField) {
						update(toRemove, fullContent, lastAR);
						fieldParsing = true;
						fullContent.append(s);
						toRemove.add(currentAR);

					} else {
						fullContent.append(s);
						update(toRemove, fullContent, currentAR);
					}
				} else {
					fullContent.append(s);
					toRemove.add(currentAR);
				}

			} else {
				if (hasField) {
					fieldParsing = true;
					fullContent.append(s);
					toRemove.add(currentAR);
				} else {
					// Do nothing
				}
			}
			lastAR = currentAR;
		}
		update(toRemove, fullContent, lastAR);
		super.removeAll(toRemove);

	}

	private void update(Collection<BufferedElement> toRemove,
			StringBuilder fullContent, ARBufferedRegion lastAR) {
		if (fullContent.length() > 0) {
			String content = fullContent.toString();
			String itemNameList = getItemNameList(content);
			lastAR.setTContent(itemNameList != null ? itemNameList : content);
			fullContent.setLength(0);
			toRemove.remove(lastAR);
		}

	}

	private String getItemNameList(String content) {
		IDocumentFormatter formatter = document.getFormatter();
		FieldsMetadata fieldsMetadata = document.getFieldsMetadata();
		if (formatter != null && fieldsMetadata != null) {

			Collection<String> fieldsAsList = fieldsMetadata.getFieldsAsList();
			for (final String fieldName : fieldsAsList) {
				if (content.contains(fieldName)) {
					this.itemNameList = formatter.extractItemNameList(content,
							fieldName, true);
					if (StringUtils.isNotEmpty(itemNameList)) {
						if (!isIgnoreLoopDirective(itemNameList)) {
							setStartLoopDirective(formatter
									.getStartLoopDirective(itemNameList));
						}
						return formatter.formatAsFieldItemList(content,
								fieldName, true);
					}
				}
			}
		}
		return null;
	}

	public void addEndLoopDirective(String itemNameList) {
		IDocumentFormatter formatter = document.getFormatter();
		this.endTagElement
				.setAfter(formatter.getEndLoopDirective(itemNameList));
	}

	private void setStartLoopDirective(String startLoopDirective) {
		this.startTagElement.setBefore(startLoopDirective);
	}

	public String getItemNameList() {
		return itemNameList;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public void addIgnoreLoopDirective(String itemNameList) {
		if (ignoreLoopDirective == null) {
			ignoreLoopDirective = new ArrayList<String>();

		}
		ignoreLoopDirective.add(itemNameList);
	}

	public boolean isIgnoreLoopDirective(String itemNameList) {
		if (ignoreLoopDirective == null) {
			return false;
		}
		return ignoreLoopDirective.contains(itemNameList);
	}

}
